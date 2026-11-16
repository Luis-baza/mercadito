(() => {
    const root = document.querySelector("[data-chatbot]");
    if (!root) return;

    const panel = root.querySelector("[data-chat-panel]");
    const toggleButton = root.querySelector("[data-chat-toggle]");
    const messages = root.querySelector("[data-chat-messages]");
    const form = root.querySelector("[data-chat-form]");
    const input = root.querySelector("[data-chat-input]");
    const sendButton = root.querySelector("[data-chat-send]");
    const status = root.querySelector("[data-chat-status]");
    const initialGreeting = root.querySelector("[data-chat-greeting]").textContent;

    const appendMessage = (text, sender) => {
        const row = document.createElement("p");
        row.className = `chatbot-message chatbot-message-${sender}`;
        const bubble = document.createElement("span");
        bubble.className = "chatbot-bubble";
        bubble.textContent = text;
        row.appendChild(bubble);
        messages.appendChild(row);
        messages.scrollTop = messages.scrollHeight;
    };

    const setOpen = (open) => {
        panel.hidden = !open;
        toggleButton.setAttribute("aria-expanded", String(open));
        if (open) input.focus();
    };

    const csrfHeaders = () => {
        const headers = { "Content-Type": "application/json" };
        if (root.dataset.csrfHeader && root.dataset.csrfToken) {
            headers[root.dataset.csrfHeader] = root.dataset.csrfToken;
        }
        return headers;
    };

    toggleButton.addEventListener("click", () => setOpen(panel.hidden));
    root.querySelector("[data-chat-close]").addEventListener("click", () => setOpen(false));

    root.querySelector("[data-chat-reset]").addEventListener("click", async (event) => {
        const button = event.currentTarget;
        button.disabled = true;
        try {
            const response = await fetch(root.dataset.resetUrl, {
                method: "POST",
                headers: csrfHeaders()
            });
            if (!response.ok) throw new Error("No se pudo reiniciar la conversación.");
            messages.replaceChildren();
            appendMessage(initialGreeting, "bot");
        } catch (error) {
            appendMessage(error.message || "No se pudo reiniciar el chat.", "bot");
        } finally {
            button.disabled = false;
        }
    });

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const mensaje = input.value.trim();
        if (!mensaje || sendButton.disabled) return;

        appendMessage(mensaje, "user");
        input.value = "";
        input.disabled = true;
        sendButton.disabled = true;
        status.hidden = false;

        try {
            const response = await fetch(root.dataset.messageUrl, {
                method: "POST",
                headers: csrfHeaders(),
                body: JSON.stringify({ mensaje })
            });
            const body = await response.json().catch(() => ({}));
            if (!response.ok) {
                throw new Error(body.error || "No se pudo enviar el mensaje. Inténtalo de nuevo.");
            }
            appendMessage(body.respuesta || "No recibí una respuesta. Inténtalo de nuevo.", "bot");
        } catch (error) {
            appendMessage(error.message || "No fue posible conectar con el asistente.", "bot");
        } finally {
            status.hidden = true;
            input.disabled = false;
            sendButton.disabled = false;
            input.focus();
        }
    });
})();
