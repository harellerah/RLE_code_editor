const BASE_URL = "http://localhost:8080";
let token = localStorage.getItem("token");

async function login() {
  const res = await fetch(`${BASE_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      username: document.getElementById("username").value,
      password: document.getElementById("password").value,
    }),
  });

  if (res.ok) {
    const data = await res.json();
    localStorage.setItem("token", data.token);
    window.location.href = "dashboard.html";
  } else {
    document.getElementById("msg").innerText = "Login failed.";
  }
}

async function register() {
  const res = await fetch(`${BASE_URL}/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      username: document.getElementById("username").value,
      password: document.getElementById("password").value,
    }),
  });

  if (res.ok) {
    document.getElementById("msg").innerText = "Registered. You can now login.";
  } else {
    document.getElementById("msg").innerText = "Registration failed.";
  }
}

async function uploadFile() {
  const fileInput = document.getElementById("fileInput");
  if (!fileInput.files.length) return alert("Choose a file!");

  const formData = new FormData();
  formData.append("file", fileInput.files[0]);
  console.log("Using token:", localStorage.getItem("token"));

  const res = await fetch(`${BASE_URL}/files/upload`, {
    method: "POST",
    headers: { Authorization: "Bearer " + localStorage.getItem("token") },
    body: formData,
  });

  const text = await res.text();
  document.getElementById(
    "uploadStatus"
  ).innerText = `Uploaded! File ID: ${text}`;
}

async function downloadFile() {
  const id = document.getElementById("fileId").value;
  const res = await fetch(`${BASE_URL}/files/download/${id}`, {
    headers: { Authorization: "Bearer " + localStorage.getItem("token") },
  });

  if (res.ok) {
    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "downloaded_file";
    document.body.appendChild(a);
    a.click();
    a.remove();
  } else {
    alert("Download failed.");
  }
}
