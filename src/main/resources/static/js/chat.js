document.addEventListener("DOMContentLoaded", function () {
    const userId = document.getElementById("userId").value;
    const chatBox = document.getElementById("chat-box");
    const messageInput = document.getElementById("message");
    const sendBtn = document.getElementById("send-btn");

    loadHistory();

    sendBtn.addEventListener("click", sendMessage);
    messageInput.addEventListener("keydown", function (e) {
        if (e.key === "Enter") {
            sendMessage();
        }
    });

    function appendMessage(text, type) {
        const div = document.createElement("div");
        div.classList.add("message");
        if (type === "user") {
            div.classList.add("msg-user");
            div.textContent = "Tú: " + text;
        } else {
            div.classList.add("msg-agent");
            div.textContent = "Agente: " + text;
        }
        chatBox.appendChild(div);
        chatBox.scrollTop = chatBox.scrollHeight;
    }

    function sendMessage() {
        const text = messageInput.value.trim();
        if (!text) return;

        appendMessage(text, "user");
        messageInput.value = "";
        sendBtn.disabled = true;

        const formData = new URLSearchParams();
        formData.append("userId", userId);
        formData.append("message", text);

        fetch("/chat/send", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
            },
            body: formData.toString()
        })
            .then(response => response.text())
            .then(data => {
                appendMessage(data, "agent");
                sendBtn.disabled = false;
            })
            .catch(err => {
                console.error(err);
                appendMessage("Error al comunicarse con el servidor.", "agent");
                sendBtn.disabled = false;
            });
    }

    function loadHistory() {
        fetch("/chat/history?userId=" + encodeURIComponent(userId))
            .then(response => response.json())
            .then(history => {
                history.forEach(conv => {
                    appendMessage(conv.userMessage, "user");
                    appendMessage(conv.agentResponse, "agent");
                });
            })
            .catch(err => {
                console.error("Error cargando historial:", err);
            });
    }
});
