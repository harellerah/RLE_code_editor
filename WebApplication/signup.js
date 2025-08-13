document.getElementById("signupForm").addEventListener("submit", async function (e) {
    e.preventDefault();

    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value.trim();
    const confirmPassword = document.getElementById("confirmPassword").value.trim();
    const role = document.getElementById("role").value;

    if (password !== confirmPassword) {
        document.getElementById("confirmPassword").style.border = "2px solid red";

        let errorEl = document.createElement("div");
        errorEl.textContent = "Passwords do not match";
        errorEl.style.color = "red";
        errorEl.style.fontSize = "0.9em";
        document.getElementById("confirmPassword").insertAdjacentElement("afterend", errorEl);
        return;
    }

    try {
        const response = await fetch("http://localhost:8080/auth/register", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password, role })
        });

        const data = await response;

        if (data.ok) {
            alert("Account created! You can now log in.");
            window.location.href = "index.html";
        } else {
            const errorText = await response.text();
            alert(`Error ${response.status}: ${errorText}`);
        }
    } catch (error) {
        console.error("Error:", error);
        alert("An error occurred. Please try again.");
    }
});
