document.addEventListener('DOMContentLoaded', () => {
    const overlay = document.getElementById('imagePreviewOverlay');
    const target = document.getElementById('imagePreviewTarget');

    document.querySelectorAll('.previewable-image').forEach(img => {
        img.addEventListener('click', () => {
            target.src = img.src;
            overlay.classList.add('active');
        });
    });

    overlay.addEventListener('click', () => {
        overlay.classList.remove('active');
    });
});

