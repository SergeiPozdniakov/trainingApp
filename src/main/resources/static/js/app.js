// Функция для подтверждения удаления
function confirmDelete(url, message) {
    if (confirm(message || 'Вы уверены, что хотите удалить этот элемент?')) {
        fetch(url, {
            method: 'POST',
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        }).then(response => {
            if (response.ok) {
                window.location.reload();
            } else {
                alert('Ошибка при удалении');
            }
        }).catch(error => {
            console.error('Error:', error);
            alert('Ошибка при удалении');
        });
    }
}

// Автоматическое скрытие алертов
document.addEventListener('DOMContentLoaded', function() {
    // Автоматически скрываем алерты через 5 секунд
    setTimeout(function() {
        var alerts = document.querySelectorAll('.alert:not(.alert-permanent)');
        alerts.forEach(function(alert) {
            var bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        });
    }, 5000);

    // Подтверждение перед уходом со страницы с несохраненными изменениями
    window.addEventListener('beforeunload', function (e) {
        var forms = document.querySelectorAll('form[data-unsaved]');
        var hasUnsavedChanges = false;

        forms.forEach(function(form) {
            if (form.hasAttribute('data-unsaved')) {
                hasUnsavedChanges = true;
            }
        });

        if (hasUnsavedChanges) {
            e.preventDefault();
            e.returnValue = '';
        }
    });

    // Отметка формы как имеющей несохраненные изменения
    document.querySelectorAll('form').forEach(function(form) {
        form.addEventListener('change', function() {
            form.setAttribute('data-unsaved', 'true');
        });

        form.addEventListener('submit', function() {
            form.removeAttribute('data-unsaved');
        });
    });
});

// Функция для динамического обновления цвета ячейки на основе даты
function updateCellColor(cell, dueDate, applicable) {
    if (!applicable) {
        cell.classList.add('cell-gray');
        return;
    }

    const today = new Date();
    const due = new Date(dueDate);
    const warningDate = new Date(due);
    warningDate.setMonth(warningDate.getMonth() - 3);

    cell.classList.remove('cell-green', 'cell-red', 'cell-orange', 'cell-gray');

    if (warningDate < today && due > today) {
        cell.classList.add('cell-red');
    } else if (due > today) {
        cell.classList.add('cell-green');
    } else {
        cell.classList.add('cell-orange');
    }
}

// Функция для загрузки файла с прогрессом
function uploadFileWithProgress(formId, progressBarId) {
    const form = document.getElementById(formId);
    const progressBar = document.getElementById(progressBarId);

    if (form && progressBar) {
        const formData = new FormData(form);

        fetch(form.action, {
            method: 'POST',
            body: formData
        }).then(response => {
            if (response.ok) {
                window.location.href = response.url;
            }
        }).catch(error => {
            console.error('Upload error:', error);
            alert('Ошибка при загрузке файла');
        });
    }
}

// Инициализация tooltips Bootstrap
document.addEventListener('DOMContentLoaded', function() {
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
});