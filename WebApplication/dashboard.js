
const userRole = localStorage.getItem("role");
const username = localStorage.getItem("username");
const token = localStorage.getItem("authToken");
const baseUrl = "http://localhost:8080"

if (!token) {
    window.location.href = "login.html";
}

document.getElementById("welcome").textContent = `Welcome, ${username} (${userRole})`;

// let userRole = null;

// Fetch user info & data
async function initDashboard() {
    try {
        // const userRes = await fetch("/api/user/me", {
        //     headers: { "Authorization": `Bearer ${token}` }
        // });
        // const userData = await userRes.json();
        // userRole = userData.role;

        if (userRole === "teacher") {
            document.getElementById("uploadAssignmentSection").classList.remove("hidden");
        } else {
            document.getElementById("submitSolutionSection").classList.remove("hidden");
        }

        await loadAssignments();
        await loadSubmissions();
    } catch (err) {
        console.error(err);
        alert("Error loading dashboard.");
    }
}

async function loadAssignments() {
    const res = await fetch(baseUrl + "/files/assignments", {
        headers: { "Authorization": `Bearer ${token}` }
    });

    if (!res.ok) {
        console.error("Failed to load assignments:", res.statusText);
        return;
    }

    const assignments = await res.json();

    const list = document.getElementById("assignmentsList");
    list.innerHTML = "";

    assignments.forEach((filename) => {
        const li = document.createElement("li");
        li.className = "flex items-center justify-between";

        const textSpan = document.createElement("span");
        textSpan.textContent = filename;

        // Create download button
        const downloadBtn = document.createElement("button");
        downloadBtn.textContent = "Download";
        downloadBtn.className = "bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded";

        downloadBtn.addEventListener("click", () => {
            downloadFile(filename)
        });
        const deleteBtn = document.createElement("button");
        deleteBtn.textContent = "Delete";
        deleteBtn.className = "ml-2 bg-red-500 text-white px-2 py-1 rounded";
        deleteBtn.onclick = () => deleteFile(filename);

        li.appendChild(textSpan);
        if (userRole === "teacher")
            li.appendChild(deleteBtn);
        li.appendChild(downloadBtn);
        list.appendChild(li);

        if (userRole === "student") {
            const option = document.createElement("option");
            option.value = filename;
            option.textContent = filename;
            document.querySelector('select[name="assignment"]').appendChild(option);
        }
    });
}

async function loadSubmissions() {
    const res = await fetch(baseUrl+`/files/submissions?role=${userRole}&username=${username}`, {
        headers: { "Authorization": `Bearer ${token}` }
    });
    const submissions = await res.json();
    const list = document.getElementById("submissionsList");
    list.innerHTML = "";
    console.log(submissions)
    submissions.forEach(s => {
        const li = document.createElement("li");
        li.className = "flex items-center justify-between";

        const textSpan = document.createElement("span");
        textSpan.textContent = userRole === "teacher" ? `${s.metadata.uploader}: ${s.filename} for ${s.metadata.assignment}` : `${s.filename} for ${s.metadata.assignment}`;

        // Create download button
        const downloadBtn = document.createElement("button");
        downloadBtn.textContent = "Download";
        downloadBtn.className = "bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded";

        downloadBtn.addEventListener("click", () => {
            downloadFile(s.filename)
        });

        const deleteBtn = document.createElement("button");
        deleteBtn.textContent = "Delete";
        deleteBtn.className = "ml-2 bg-red-500 text-white px-2 py-1 rounded";
        deleteBtn.onclick = () => deleteFile(s.filename);

        li.appendChild(textSpan);
        if (userRole === "student")
            li.appendChild(deleteBtn);
        li.appendChild(downloadBtn);
        list.appendChild(li);
    });
}

// Upload assignment (teacher)
document.getElementById("uploadAssignmentForm")?.addEventListener("submit", async e => {
    e.preventDefault();
    const file = document.getElementById("assignmentFile").files[0];
    const formData = new FormData();
    formData.append("file", file);
    formData.append("role", userRole);

    const res = await fetch(baseUrl+"/files/assignments/upload", {
        method: "POST",
        headers: { "Authorization": `Bearer ${token}` },
        body: formData
    });

    if (res.ok) {
        alert("Assignment uploaded!");
        loadAssignments();
    } else {
        alert("Upload failed.");
    }
});

// Submit solution (student)
document.getElementById("submitSolutionForm")?.addEventListener("submit", async e => {
    e.preventDefault();
    const select = document.querySelector('select[name="assignment"]');
    const assignment = select.options[select.selectedIndex].text;  // The visible text
    const file = document.getElementById("solutionFile").files[0];
    const formData = new FormData();
    formData.append("uploader", username);
    formData.append("role", userRole);
    formData.append("file", file);
    formData.append("type", "submission");
    formData.append("assignment", assignment);

    const res = await fetch(baseUrl+"/files/upload", {
        method: "POST",
        headers: { "Authorization": `Bearer ${token}` },
        body: formData
    });

    if (res.ok) {
        alert("Solution submitted!");
        loadSubmissions();
    } else {
        alert("Submission failed.");
    }
});

async function downloadFile(filename) {
    const res = await fetch(`${baseUrl}/files/download/${encodeURIComponent(filename)}?user=${username  }&role=${userRole}`, {
        headers: { "Authorization": `Bearer ${token}` }
    });

    if (!res.ok) {
        alert("Failed to download file");
        return;
    }

    const blob = await res.blob(); // get binary data as Blob
    const url = window.URL.createObjectURL(blob);

    // Create a temporary <a> to trigger download
    const a = document.createElement("a");
    a.href = url;
    a.download = filename; // suggest file name
    document.body.appendChild(a);
    a.click();

    // Clean up
    a.remove();
    window.URL.revokeObjectURL(url);
}

async function deleteFile(filename) {
    let endpoint = "";
    if (userRole === "teacher") {
        endpoint = `/files/assignments/${encodeURIComponent(filename)}`;
    } else {
        endpoint = `/files/submissions/${encodeURIComponent(filename)}`;
    }

    const res = await fetch(baseUrl + endpoint + `?username=${username}&role=${userRole}`, {
        method: "DELETE",
        headers: {"Authorization": `Bearer ${token}`}
    });

    if (res.ok) {
        alert(await res.text());
        if (userRole === "teacher") {
            await loadAssignments();
        } else {
            await loadSubmissions();
        }
    } else {
        alert("Error deleting file");
    }
}

initDashboard();

