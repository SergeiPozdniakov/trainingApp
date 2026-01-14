/**
 * Training Management System - Animations
 * Version 1.0
 */

document.addEventListener('DOMContentLoaded', function() {
    // Инициализация частиц для фона
    initParticles();

    // Инициализация анимаций при прокрутке
    initScrollAnimations();

    // Инициализация интерактивных элементов
    initInteractiveElements();

    // Инициализация таблиц с обучением
    initTrainingTables();
});

function initParticles() {
    const container = document.querySelector('.main-background .floating-shapes');
    if (!container) return;

    const colors = [
        'rgba(99, 102, 241, 0.1)',
        'rgba(139, 92, 246, 0.1)',
        'rgba(6, 182, 212, 0.1)',
        'rgba(16, 185, 129, 0.1)'
    ];

    for (let i = 0; i < 15; i++) {
        const shape = document.createElement('div');
        const size = Math.random() * 100 + 50;
        const color = colors[Math.floor(Math.random() * colors.length)];
        const duration = Math.random() * 20 + 20;
        const delay = Math.random() * 10;

        shape.style.width = `${size}px`;
        shape.style.height = `${size}px`;
        shape.style.background = color;
        shape.style.left = `${Math.random() * 100}%`;
        shape.style.top = `${Math.random() * 100}%`;
        shape.style.animationDuration = `${duration}s`;
        shape.style.animationDelay = `${delay}s`;
        shape.style.opacity = Math.random() * 0.3 + 0.1;

        container.appendChild(shape);
    }
}

function initScrollAnimations() {
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('slide-in');
            }
        });
    }, observerOptions);

    // Наблюдаем за карточками
    document.querySelectorAll('.card:not(.auth-card)').forEach(card => {
        observer.observe(card);
    });

    // Наблюдаем за статистикой
    document.querySelectorAll('.stat-card').forEach(stat => {
        observer.observe(stat);
    });
}

function initInteractiveElements() {
    // Анимация кнопок
    document.querySelectorAll('.btn').forEach(btn => {
        btn.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-3px) scale(1.02)';
        });

        btn.addEventListener('mouseleave', function() {
            this.style.transform = '';
        });

        btn.addEventListener('mousedown', function() {
            this.style.transform = 'translateY(1px) scale(0.98)';
        });

        btn.addEventListener('mouseup', function() {
            this.style.transform = 'translateY(-3px) scale(1.02)';
        });
    });

    // Анимация карточек
    document.querySelectorAll('.card').forEach(card => {
        card.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-8px)';
        });

        card.addEventListener('mouseleave', function() {
            this.style.transform = '';
        });
    });

    // Плавное появление элементов форм
    document.querySelectorAll('.form-control').forEach(input => {
        input.addEventListener('focus', function() {
            this.parentElement.classList.add('focused');
        });

        input.addEventListener('blur', function() {
            if (!this.value) {
                this.parentElement.classList.remove('focused');
            }
        });
    });
}

function initTrainingTables() {
    // Обновление цвета ячеек при изменении данных
    document.addEventListener('change', function(e) {
        if (e.target.matches('input[type="date"], input[type="radio"]')) {
            updateTrainingCellColors(e.target.closest('.training-cell'));
        }
    });

    // Инициализация цветов при загрузке
    document.querySelectorAll('.training-cell').forEach(updateTrainingCellColors);

    // Drag and drop для файлов
    document.querySelectorAll('input[type="file"]').forEach(input => {
        const label = input.previousElementSibling;

        input.addEventListener('change', function() {
            if (this.files.length > 0) {
                const fileName = this.files[0].name;
                label.innerHTML = `<i class="fas fa-check text-success me-1"></i>${fileName}`;
                label.classList.add('bg-success', 'text-white');
            }
        });

        label.addEventListener('dragover', function(e) {
            e.preventDefault();
            this.classList.add('drag-over');
        });

        label.addEventListener('dragleave', function() {
            this.classList.remove('drag-over');
        });

        label.addEventListener('drop', function(e) {
            e.preventDefault();
            this.classList.remove('drag-over');

            if (e.dataTransfer.files.length > 0) {
                input.files = e.dataTransfer.files;
                input.dispatchEvent(new Event('change'));
            }
        });
    });
}

function updateTrainingCellColors(cell) {
    if (!cell) return;

    // Удаляем все статусные классы
    cell.classList.remove('status-valid', 'status-warning', 'status-expired', 'status-inapplicable');

    const dateInput = cell.querySelector('input[type="date"]');
    const notApplicableRadio = cell.querySelector('input[value="false"]:checked');

    // Если выбрано "Неприменимо"
    if (notApplicableRadio) {
        cell.classList.add('status-inapplicable');
        return;
    }

    // Проверяем дату
    if (dateInput && dateInput.value) {
        const examDate = new Date(dateInput.value);
        const validityMonths = parseInt(cell.dataset.validityMonths) || 12;
        const nextExamDate = new Date(examDate);
        nextExamDate.setMonth(nextExamDate.getMonth() + validityMonths);

        const threeMonthsBefore = new Date(nextExamDate);
        threeMonthsBefore.setMonth(threeMonthsBefore.getMonth() - 3);

        const today = new Date();

        if (today > nextExamDate) {
            cell.classList.add('status-expired');
            // Добавляем анимацию для просроченных
            cell.classList.add('pulse');
        } else if (today > threeMonthsBefore) {
            cell.classList.add('status-warning');
        } else {
            cell.classList.add('status-valid');
        }
    }
}

// Утилиты для работы с датами
window.formatDate = function(date) {
    return new Date(date).toLocaleDateString('ru-RU', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
    });
};

window.calculateNextExam = function(examDate, validityMonths) {
    const date = new Date(examDate);
    date.setMonth(date.getMonth() + validityMonths);
    return date;
};

window.isExpiringSoon = function(examDate, validityMonths) {
    const nextExam = window.calculateNextExam(examDate, validityMonths);
    const threeMonthsBefore = new Date(nextExam);
    threeMonthsBefore.setMonth(threeMonthsBefore.getMonth() - 3);
    return new Date() > threeMonthsBefore && new Date() < nextExam;
};