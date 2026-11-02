document.addEventListener("DOMContentLoaded", () => {
    const navToggle = document.querySelector("[data-nav-toggle]");
    const nav = document.querySelector("[data-site-nav]");

    if (navToggle && nav) {
        navToggle.addEventListener("click", () => {
            const expanded = navToggle.getAttribute("aria-expanded") === "true";
            navToggle.setAttribute("aria-expanded", String(!expanded));
            nav.classList.toggle("is-open", !expanded);
        });
    }

    document.querySelectorAll("[data-confirm]").forEach((form) => {
        form.addEventListener("submit", (event) => {
            const message = form.dataset.confirm;
            if (message && !window.confirm(message)) {
                event.preventDefault();
            }
        });
    });

    document.querySelectorAll("[data-password-toggle]").forEach((button) => {
        button.addEventListener("click", () => {
            const input = document.getElementById(button.dataset.passwordToggle);
            if (!input) return;
            const visible = input.type === "text";
            input.type = visible ? "password" : "text";
            button.setAttribute("aria-pressed", String(!visible));
            button.textContent = visible ? "Mostrar" : "Ocultar";
        });
    });

    document.querySelectorAll("[data-image-input]").forEach((input) => {
        input.addEventListener("change", () => {
            const preview = document.getElementById(input.dataset.imageInput);
            const file = input.files && input.files[0];
            if (!preview) return;

            if (!file) {
                preview.removeAttribute("src");
                preview.hidden = true;
                return;
            }

            if (file.type && !["image/jpeg", "image/png", "image/gif"].includes(file.type)) {
                input.value = "";
                preview.removeAttribute("src");
                preview.hidden = true;
                window.alert("Selecciona una imagen JPG, PNG o GIF.");
                return;
            }
            if (file.size > 5 * 1024 * 1024) {
                input.value = "";
                preview.removeAttribute("src");
                preview.hidden = true;
                window.alert("La imagen debe pesar 5 MB o menos.");
                return;
            }

            if (preview.dataset.objectUrl) {
                URL.revokeObjectURL(preview.dataset.objectUrl);
            }
            const objectUrl = URL.createObjectURL(file);
            preview.dataset.objectUrl = objectUrl;
            preview.src = objectUrl;
            preview.hidden = false;
        });
    });
});
