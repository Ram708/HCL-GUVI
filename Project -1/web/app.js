/* Student Management System – front end.
 * Talks to the Java backend (com.sms.web.WebServer) through its JSON API. */
(function () {
  'use strict';

  // ---------------------------------------------------------------- state

  const state = {
    q: '',
    course: '',
    sort: 'id',
    order: 'asc',
    editingId: null,   // null = adding a new student
    deletingId: null,
    lastChangedId: null
  };

  const $ = (sel) => document.querySelector(sel);

  const el = {
    tableBody: $('#tableBody'),
    emptyState: $('#emptyState'),
    emptyText: $('#emptyText'),
    tableFooter: $('#tableFooter'),
    searchInput: $('#searchInput'),
    courseFilter: $('#courseFilter'),
    sortField: $('#sortField'),
    orderBtn: $('#orderBtn'),
    orderIcon: $('#orderIcon'),
    courseList: $('#courseList'),
    courseOptions: $('#courseOptions'),
    gradeChart: $('#gradeChart'),
    tooltip: $('#chartTooltip'),
    studentModal: $('#studentModal'),
    studentForm: $('#studentForm'),
    modalTitle: $('#modalTitle'),
    formError: $('#formError'),
    saveBtn: $('#saveBtn'),
    confirmModal: $('#confirmModal'),
    confirmText: $('#confirmText'),
    confirmDeleteBtn: $('#confirmDeleteBtn'),
    toastStack: $('#toastStack')
  };

  // ------------------------------------------------------------------ API

  async function api(method, url, data) {
    const options = { method, headers: {} };
    if (data) {
      options.body = new URLSearchParams(data).toString();
      options.headers['Content-Type'] = 'application/x-www-form-urlencoded; charset=UTF-8';
    }
    let res;
    try {
      res = await fetch(url, options);
    } catch (e) {
      throw { error: 'Cannot reach the server. Is the Java web server running?' };
    }
    const body = await res.json().catch(() => ({}));
    if (!res.ok) throw body;
    return body;
  }

  // ------------------------------------------------------------- helpers

  function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, (c) => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
  }

  function fmt(n) {
    return Number(n).toFixed(2).replace(/\.00$/, '');
  }

  function debounce(fn, ms) {
    let t;
    return (...args) => {
      clearTimeout(t);
      t = setTimeout(() => fn(...args), ms);
    };
  }

  function toast(message, type) {
    const node = document.createElement('div');
    node.className = 'toast' + (type === 'error' ? ' error' : '');
    node.setAttribute('role', type === 'error' ? 'alert' : 'status');
    node.textContent = message;
    el.toastStack.appendChild(node);
    setTimeout(() => {
      node.classList.add('leaving');
      setTimeout(() => node.remove(), 250);
    }, 3200);
  }

  const ICON_PASS = '<svg viewBox="0 0 16 16" width="12" height="12" aria-hidden="true"><path d="m3.5 8.5 3 3 6-7" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg>';
  const ICON_FAIL = '<svg viewBox="0 0 16 16" width="12" height="12" aria-hidden="true"><path d="m4.5 4.5 7 7m0-7-7 7" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/></svg>';

  // ------------------------------------------------------------ loading

  async function loadStudents() {
    const params = new URLSearchParams({
      q: state.q, course: state.course, sort: state.sort, order: state.order
    });
    try {
      const students = await api('GET', '/api/students?' + params);
      renderTable(students);
    } catch (err) {
      toast(err.error || 'Could not load students', 'error');
    }
  }

  async function loadStats() {
    try {
      const stats = await api('GET', '/api/stats');
      renderStats(stats);
      renderChart(stats.byGrade);
      renderCourses(stats.byCourse, stats.total);
    } catch (err) {
      toast(err.error || 'Could not load statistics', 'error');
    }
  }

  function refresh() {
    return Promise.all([loadStudents(), loadStats()]);
  }

  // ---------------------------------------------------------- rendering

  function renderTable(students) {
    el.tableBody.innerHTML = students.map((s) => `
      <tr data-id="${escapeHtml(s.studentId)}"${s.studentId === state.lastChangedId ? ' class="flash"' : ''}>
        <td class="cell-id">${escapeHtml(s.studentId)}</td>
        <td>${escapeHtml(s.name)}</td>
        <td class="num">${s.age}</td>
        <td class="cell-muted">${escapeHtml(s.email)}</td>
        <td class="cell-muted">${escapeHtml(s.phone)}</td>
        <td>${escapeHtml(s.course)}</td>
        <td class="num">${fmt(s.marks)}</td>
        <td><span class="grade">${escapeHtml(s.grade)}</span></td>
        <td>${s.passed
          ? `<span class="result result-pass">${ICON_PASS}Pass</span>`
          : `<span class="result result-fail">${ICON_FAIL}Fail</span>`}</td>
        <td>
          <div class="row-actions">
            <button class="btn btn-ghost btn-sm" type="button" data-action="edit">Edit</button>
            <button class="btn btn-ghost btn-sm" type="button" data-action="delete">Delete</button>
          </div>
        </td>
      </tr>`).join('');
    state.lastChangedId = null;

    const filtered = state.q || state.course;
    el.emptyState.hidden = students.length > 0;
    el.emptyText.textContent = filtered
      ? 'No students match your search or filter.'
      : 'Click “Add Student” to create the first record.';
    el.tableFooter.textContent = students.length
      ? `Showing ${students.length} student${students.length === 1 ? '' : 's'}` +
        (state.course ? ` in ${state.course}` : '') +
        (state.q ? ` matching “${state.q}”` : '')
      : '';

    document.querySelectorAll('th.sortable').forEach((th) => {
      const active = th.dataset.sort === state.sort;
      th.classList.toggle('sorted', active);
      th.classList.toggle('desc', active && state.order === 'desc');
      th.setAttribute('aria-sort', active ? (state.order === 'desc' ? 'descending' : 'ascending') : 'none');
    });
  }

  function renderStats(s) {
    $('#statTotal').textContent = s.total;
    $('#statCourses').textContent = `${s.byCourse.length} course${s.byCourse.length === 1 ? '' : 's'}`;
    $('#statAvg').textContent = s.total ? fmt(s.average) : '–';
    $('#statRange').textContent = s.total ? `Range ${fmt(s.lowest)} – ${fmt(s.highest)}` : 'No records yet';
    $('#statPass').textContent = s.total ? `${fmt(s.passRate)}%` : '–';
    $('#statPassNote').textContent = s.total ? `${s.passed} passed · ${s.failed} failed` : ' ';
    $('#statTopper').textContent = s.topper ? s.topper.name : '–';
    $('#statTopper').title = s.topper ? s.topper.name : '';
    $('#statTopperNote').textContent = s.topper
      ? `${s.topper.studentId} · ${fmt(s.topper.marks)} marks · ${s.topper.course}` : ' ';
  }

  function niceTicks(max) {
    if (max <= 0) return { top: 4, step: 1 };
    const step = Math.max(1, Math.ceil(max / 4));
    return { top: step * Math.ceil(max / step), step };
  }

  function renderChart(byGrade) {
    const max = Math.max(0, ...byGrade.map((g) => g.count));
    const { top, step } = niceTicks(max);

    let axis = '';
    let grid = '';
    for (let v = 0; v <= top; v += step) {
      const pct = (v / top) * 100;
      axis += `<span style="bottom:${pct}%">${v}</span>`;
      grid += `<div class="${v === 0 ? 'baseline' : ''}" style="bottom:${pct}%"></div>`;
    }

    const cols = byGrade.map((g) => {
      const pct = (g.count / top) * 100;
      return `
        <div class="chart-col" tabindex="0" data-label="${escapeHtml(g.label)}" data-count="${g.count}"
             aria-label="Grade ${escapeHtml(g.label)}: ${g.count} student${g.count === 1 ? '' : 's'}">
          <div class="bar-area"><div class="bar${g.count ? '' : ' zero'}" style="height:${pct}%"></div></div>
          <div class="bar-label">${escapeHtml(g.label)}</div>
        </div>`;
    }).join('');

    el.gradeChart.innerHTML = `
      <div class="chart-axis">${axis}</div>
      <div class="chart-plot"><div class="chart-grid">${grid}</div>${cols}</div>`;
    el.gradeChart.setAttribute('aria-label', 'Grade distribution: ' +
      byGrade.map((g) => `${g.label} ${g.count}`).join(', '));
  }

  function showTooltip(col) {
    const panel = el.gradeChart.closest('.panel').getBoundingClientRect();
    const bar = col.querySelector('.bar').getBoundingClientRect();
    const area = col.querySelector('.bar-area').getBoundingClientRect();
    const count = Number(col.dataset.count);
    const top = count ? bar.top : area.bottom;
    el.tooltip.innerHTML = `Grade <strong>${escapeHtml(col.dataset.label)}</strong> · ` +
      `${count} student${count === 1 ? '' : 's'}`;
    el.tooltip.style.left = (bar.left + bar.width / 2 - panel.left) + 'px';
    el.tooltip.style.top = (top - panel.top - 8) + 'px';
    el.tooltip.hidden = false;
  }

  function hideTooltip() {
    el.tooltip.hidden = true;
  }

  function renderCourses(byCourse, total) {
    const max = Math.max(1, ...byCourse.map((c) => c.count));
    el.courseList.innerHTML = byCourse.length
      ? byCourse.map((c) => `
        <li>
          <button class="course-item${c.label.toLowerCase() === state.course.toLowerCase() ? ' active' : ''}"
                  type="button" data-course="${escapeHtml(c.label)}"
                  aria-pressed="${c.label.toLowerCase() === state.course.toLowerCase()}">
            <span class="course-name">${escapeHtml(c.label)}</span>
            <span class="course-count">${c.count} · ${Math.round((c.count / total) * 100)}%</span>
            <span class="course-meter" aria-hidden="true"><span style="width:${(c.count / max) * 100}%"></span></span>
          </button>
        </li>`).join('')
      : '<li class="panel-hint">No courses yet.</li>';

    // Keep the filter dropdown and form suggestions in sync with the data.
    const current = state.course;
    el.courseFilter.innerHTML = '<option value="">All courses</option>' +
      byCourse.map((c) => `<option value="${escapeHtml(c.label)}">${escapeHtml(c.label)}</option>`).join('');
    el.courseFilter.value = byCourse.some((c) => c.label === current) ? current : '';
    if (current && !el.courseFilter.value) {
      state.course = '';
    }
    el.courseOptions.innerHTML = byCourse
      .map((c) => `<option value="${escapeHtml(c.label)}"></option>`).join('');
  }

  // ------------------------------------------------------- form & modal

  const rules = {
    studentId: (v) => /^[A-Za-z0-9-]{2,15}$/.test(v) ? '' : "ID must be 2-15 letters, digits or '-' (e.g. S101).",
    name: (v) => /^[A-Za-z .'-]{2,50}$/.test(v) ? '' : "Name must be 2-50 letters (spaces, . ' - allowed).",
    age: (v) => /^\d+$/.test(v) && +v >= 5 && +v <= 100 ? '' : 'Age must be a whole number between 5 and 100.',
    email: (v) => /^[\w.+-]+@[\w-]+(\.[\w-]+)+$/.test(v) ? '' : 'Enter a valid email (e.g. name@example.com).',
    phone: (v) => /^[6-9]\d{9}$/.test(v) ? '' : 'Enter a valid 10-digit mobile number starting with 6-9.',
    course: (v) => v && v.length <= 40 ? '' : 'Course is required (max 40 characters).',
    marks: (v) => v !== '' && !isNaN(v) && +v >= 0 && +v <= 100 ? '' : 'Marks must be a number between 0 and 100.'
  };

  function setFieldError(name, message) {
    const small = el.studentForm.querySelector(`[data-error="${name}"]`);
    if (!small) return;
    const field = small.closest('.field');
    small.textContent = message || '';
    field.classList.toggle('invalid', !!message);
    field.querySelector('input').setAttribute('aria-invalid', message ? 'true' : 'false');
  }

  function clearErrors() {
    Object.keys(rules).forEach((k) => setFieldError(k, ''));
    el.formError.hidden = true;
  }

  function formData() {
    const data = {};
    new FormData(el.studentForm).forEach((v, k) => { data[k] = String(v).trim(); });
    return data;
  }

  function validateForm(data) {
    let firstInvalid = null;
    Object.keys(rules).forEach((k) => {
      if (k === 'studentId' && state.editingId) return;
      const msg = rules[k](data[k] || '');
      setFieldError(k, msg);
      if (msg && !firstInvalid) firstInvalid = k;
    });
    if (firstInvalid) el.studentForm.elements[firstInvalid].focus();
    return !firstInvalid;
  }

  function openForm(student) {
    el.studentForm.reset();
    clearErrors();
    state.editingId = student ? student.studentId : null;
    el.modalTitle.textContent = student ? `Edit student ${student.studentId}` : 'Add student';
    el.saveBtn.textContent = student ? 'Save changes' : 'Add student';
    const idInput = el.studentForm.elements.studentId;
    idInput.readOnly = !!student;
    if (student) {
      ['studentId', 'name', 'age', 'email', 'phone', 'course', 'marks'].forEach((k) => {
        el.studentForm.elements[k].value = student[k];
      });
    }
    el.studentModal.showModal();
    (student ? el.studentForm.elements.name : idInput).focus();
  }

  async function submitForm(event) {
    event.preventDefault();
    const data = formData();
    if (!validateForm(data)) return;

    el.saveBtn.disabled = true;
    try {
      let saved;
      if (state.editingId) {
        saved = await api('PUT', '/api/students/' + encodeURIComponent(state.editingId), data);
        toast(`Student ${saved.studentId} updated`);
      } else {
        saved = await api('POST', '/api/students', data);
        toast(`Student ${saved.studentId} added`);
      }
      state.lastChangedId = saved.studentId;
      el.studentModal.close();
      await refresh();
    } catch (err) {
      if (err.fields) {
        Object.keys(err.fields).forEach((k) => setFieldError(k, err.fields[k]));
        const first = Object.keys(err.fields)[0];
        if (el.studentForm.elements[first]) el.studentForm.elements[first].focus();
      } else {
        el.formError.textContent = err.error || 'Something went wrong.';
        el.formError.hidden = false;
      }
    } finally {
      el.saveBtn.disabled = false;
    }
  }

  function openDelete(student) {
    state.deletingId = student.studentId;
    el.confirmText.textContent =
      `This will permanently remove ${student.name} (${student.studentId}) from the records.`;
    el.confirmModal.showModal();
    el.confirmModal.querySelector('[data-close]').focus();
  }

  async function confirmDelete() {
    const id = state.deletingId;
    el.confirmDeleteBtn.disabled = true;
    try {
      const removed = await api('DELETE', '/api/students/' + encodeURIComponent(id));
      el.confirmModal.close();
      toast(`Student ${removed.studentId} deleted`);
      await refresh();
    } catch (err) {
      el.confirmModal.close();
      toast(err.error || 'Could not delete student', 'error');
    } finally {
      el.confirmDeleteBtn.disabled = false;
    }
  }

  // --------------------------------------------------------- wiring up

  function setSort(field, order) {
    state.sort = field;
    state.order = order;
    el.sortField.value = field;
    el.orderIcon.textContent = order === 'desc' ? '↓' : '↑';
    loadStudents();
  }

  function setCourse(course) {
    state.course = course;
    el.courseFilter.value = course;
    el.courseList.querySelectorAll('.course-item').forEach((b) => {
      const on = b.dataset.course === course;
      b.classList.toggle('active', on);
      b.setAttribute('aria-pressed', on);
    });
    loadStudents();
  }

  $('#addBtn').addEventListener('click', () => openForm(null));
  el.studentForm.addEventListener('submit', submitForm);
  el.confirmDeleteBtn.addEventListener('click', confirmDelete);

  document.querySelectorAll('dialog [data-close]').forEach((btn) =>
    btn.addEventListener('click', () => btn.closest('dialog').close()));

  // Validate a field as soon as the user leaves it.
  el.studentForm.addEventListener('focusout', (e) => {
    const name = e.target.name;
    if (rules[name] && e.target.value !== '' && !e.target.readOnly) {
      setFieldError(name, rules[name](e.target.value.trim()));
    }
  });
  el.studentForm.addEventListener('input', (e) => {
    if (e.target.closest('.field.invalid') && rules[e.target.name] &&
        !rules[e.target.name](e.target.value.trim())) {
      setFieldError(e.target.name, '');
    }
  });

  el.tableBody.addEventListener('click', async (e) => {
    const btn = e.target.closest('button[data-action]');
    if (!btn) return;
    const id = btn.closest('tr').dataset.id;
    try {
      const student = await api('GET', '/api/students/' + encodeURIComponent(id));
      if (btn.dataset.action === 'edit') openForm(student);
      else openDelete(student);
    } catch (err) {
      toast(err.error || 'Could not load student', 'error');
      refresh();
    }
  });

  el.searchInput.addEventListener('input', debounce(() => {
    state.q = el.searchInput.value.trim();
    loadStudents();
  }, 200));

  el.courseFilter.addEventListener('change', () => setCourse(el.courseFilter.value));

  el.courseList.addEventListener('click', (e) => {
    const btn = e.target.closest('.course-item');
    if (!btn) return;
    setCourse(btn.dataset.course === state.course ? '' : btn.dataset.course);
  });

  el.sortField.addEventListener('change', () => setSort(el.sortField.value, state.order));
  el.orderBtn.addEventListener('click', () => setSort(state.sort, state.order === 'asc' ? 'desc' : 'asc'));

  document.querySelectorAll('th.sortable').forEach((th) => {
    th.tabIndex = 0;
    const activate = () => {
      const field = th.dataset.sort;
      setSort(field, state.sort === field && state.order === 'asc' ? 'desc' : 'asc');
    };
    th.addEventListener('click', activate);
    th.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); activate(); }
    });
  });

  el.gradeChart.addEventListener('mouseover', (e) => {
    const col = e.target.closest('.chart-col');
    if (col) showTooltip(col);
  });
  el.gradeChart.addEventListener('mouseleave', hideTooltip);
  el.gradeChart.addEventListener('focusin', (e) => {
    const col = e.target.closest('.chart-col');
    if (col) showTooltip(col);
  });
  el.gradeChart.addEventListener('focusout', hideTooltip);
  window.addEventListener('resize', hideTooltip);

  // Keyboard shortcut: "/" focuses search, "n" opens the add form.
  document.addEventListener('keydown', (e) => {
    if (document.querySelector('dialog[open]')) return;
    const typing = /INPUT|SELECT|TEXTAREA/.test(document.activeElement.tagName);
    if (e.key === '/' && !typing) { e.preventDefault(); el.searchInput.focus(); }
    if (e.key === 'n' && !typing && !e.ctrlKey && !e.metaKey) { e.preventDefault(); openForm(null); }
  });

  refresh();
})();
