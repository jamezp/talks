/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

(() => {
    "use strict";
    let showCompleted = true;

    window.addEventListener("load", () => {
        // Enable tooltips
        enableToolTips();

        loadTasks();
        const summaryBox = document.querySelector("#summary");
        const addModal = new bootstrap.Modal("#taskModal");
        addModal.hide();
        document.addEventListener("shown.bs.modal", () => {
            summaryBox.focus();
        });

        // Allow the modal to be moved
        const modal = document.querySelector("#taskModal");
        makeDraggable(modal);

        const form = document.querySelector("#task");
        // Get all the buttons
        const addTask = document.querySelector("#addTask");
        const addButton = form.querySelector("#addButton");
        const updateButton = form.querySelector("#updateButton");

        // Configure default form buttons
        addButton.addEventListener("click", () => {
            createTask();
            addModal.hide();
        });

        document.addEventListener("submit", (e) => {
            e.preventDefault();
            if (!addButton.classList.contains("d-none")) {
                createTask();
                addModal.hide();
            } else if (!updateButton.classList.contains("d-none")) {
                editTask();
                addModal.hide();
            }
        });

        updateButton.addEventListener("click", () => {
            editTask();
            addModal.hide();
        });

        addTask.addEventListener("click", () => {
            addButton.classList.remove("d-none");
            updateButton.classList.add("d-none");
            document.querySelector("#priority").value = 2;
        });

        document.addEventListener("hidden.bs.modal", () => {
            const form = document.forms.namedItem("task");
            resetForm(form);
            // Reset modal position
            if (modal) {
                modal.style.position = "";
                modal.style.left = "";
                modal.style.top = "";
            }
        });

        // Show/hide button
        const showButton = document.querySelector("#show");
        const hideButton = document.querySelector("#hide");
        showButton.addEventListener("click", () => {
            showCompleted = true;
            showButton.classList.add("d-none");
            hideButton.classList.remove("d-none");
            loadTasks();
        });
        hideButton.addEventListener("click", () => {
            showCompleted = false;
            showButton.classList.remove("d-none");
            hideButton.classList.add("d-none");
            loadTasks();
        });

        // Listen for changes to tasks
        const eventSource = new EventSource("api/subscribe");
        eventSource.onerror = (err) => {
            console.error("EventSource failed:", err);
            eventSource.close();
        };
        eventSource.addEventListener("task.persist.added", () => {
            loadTasks();
        });
        eventSource.addEventListener("task.persist.updated", () => {
            loadTasks();
        });
        eventSource.addEventListener("task.persist.removed", () => {
            loadTasks();
        });
        window.addEventListener("beforeunload", () => {
            eventSource.close();
        });
    });

    /**
     * Loads the tasks table
     */
    function loadTasks() {
        let url;
        if (showCompleted) {
            url = "api/task/";
        } else {
            url = "api/task/completed/false";
        }
        fetch(url)
            .then((r) => {
                r.json().then((json) => {
                    const tasks = document.querySelector("#tasks");
                    // Clear the table
                    tasks.replaceChildren();

                    // Create a new entry for each task
                    for (let x in json) {
                        const task = json[x];
                        createTaskRow(task, tasks);
                    }
                    enableToolTips();
                });
            });
    }

    function createTaskRow(task, tasksBody) {
        const template = document.querySelector("#rowTemplate").content.cloneNode(true)
            .querySelector("slot[name='rowContainer']");
        tasksBody.appendChild(template);
        const summaryRow = template.querySelector("[data-name='summaryRow']");
        summaryRow.setAttribute("data-task-id", task.id);
        const detailId = "detailRow" + task.id;

        const priorityCol = summaryRow.querySelector("[data-name='priority']");
        const priorityIcon = document.createElement("i");
        // bi bi-check2-circle
        if (task.completed) {
            priorityIcon.classList.add("bi", "bi-check2-circle");
        } else {
            priorityIcon.classList.add("bi", "bi-circle-fill");
        }
        priorityIcon.setAttribute("data-bs-toggle", "tooltip");
        switch (task.priority) {
            case 0:
                priorityIcon.classList.add("text-danger");
                priorityIcon.setAttribute("data-bs-title", "Urgent");
                break;
            case 1:
                priorityIcon.classList.add("text-warning");
                priorityIcon.setAttribute("data-bs-title", "Important");
                break;
            case 2:
                priorityIcon.classList.add("text-primary");
                priorityIcon.setAttribute("data-bs-title", "Normal");
                break;
            case 3:
                priorityIcon.classList.add("text-info-emphasis");
                priorityIcon.setAttribute("data-bs-title", "Low");
                break;
            default:
                priorityIcon.classList.add("text-primary");
                break;
        }
        priorityCol.appendChild(priorityIcon);

        // Create the checkbox
        const completedCol = summaryRow.querySelector("[data-name='completed']");
        const completedBox = completedCol.querySelector("input[type='checkbox']");
        completedCol.addEventListener("click", () => {
            completedBox.toggleAttribute("checked");
            let json = {};
            json.id = task.id;
            json.completed = completedBox.checked;
            completeTask(json);
        });

        // Create the summary column
        const summaryColumn = summaryRow.querySelector("[data-name='summary']");
        if (task.completed) {
            completedBox.setAttribute("checked", "checked");
            const del = document.createElement("del");
            del.textContent = task.summary;
            summaryColumn.appendChild(del);
        } else {
            summaryColumn.textContent = task.summary;
        }
        summaryColumn.addEventListener("click", () => {
            const c = bootstrap.Collapse.getOrCreateInstance("#" + detailId);
            c.toggle();
        });

        // Get the edit button
        const editButton = summaryRow.querySelector("button[name='edit']");
        editButton.addEventListener("click", () => {
            const addButton = document.querySelector("#addButton");
            const updateButton = document.querySelector("#updateButton");
            addButton.classList.add("d-none");
            updateButton.classList.remove("d-none");
            loadTaskDetail(task.id);
        });

        // Get the delete button
        const deleteButton = summaryRow.querySelector("button[name='delete']");
        const deleteItem = deleteButton.querySelector("i");
        deleteItem.setAttribute("data-task-id", task.id);
        const modal = document.querySelector("#confirmDelete");
        modal.addEventListener("show.bs.modal", event => {
            // Button that triggered the modal
            const button = event.relatedTarget;
            const taskId = button.getAttribute("data-task-id");

            const deleteButton = document.querySelector("#deleteButton");
            deleteButton.onclick = () => {
                fetch(`api/task/${taskId}`, {
                    method: "DELETE"
                }).then(response => {
                    // Hide the modal
                    bootstrap.Modal.getInstance("#confirmDelete").hide();
                    if (response.status === 200) {
                        response.json().then((json) => {
                            success(`Successfully deleted ${json.summary}`);
                            loadTasks();
                        });
                    } else {
                        response.text().then(t => {
                            error(`Failed to delete task. Reason: ${t}`);
                        });
                    }
                }).catch(reason => {
                    console.error(reason);
                });
            };
        });

        // Create the detail row
        const detailRow = template.querySelector("[data-name='detailRow']");
        detailRow.id = detailId;
        const addedDate = new Date(task.added);
        detailRow.querySelector("slot[name='created']").textContent = addedDate.toLocaleString();
        detailRow.querySelector("slot[name='detail']").textContent = task.description;
        if (task.updated) {
            const updatedDate = new Date(task.updated);
            detailRow.querySelector("slot[name='updated']").textContent = updatedDate.toLocaleString();
        }
    }

    /**
     * Load a task detail
     * @param id the task id
     */
    function loadTaskDetail(id) {
        const form = document.forms.namedItem("task");
        fetch("api/task/" + id).then(response => {
            response.json().then((json) => {
                for (const key in json) {
                    const input = form.querySelector(`input[name='${key}']`);
                    if (input) {
                        input.value = json[key];
                    }
                    const textArea = form.querySelector(`textarea[name='${key}']`);
                    if (textArea) {
                        textArea.textContent = json[key];
                    }
                    const select = form.querySelector(`select[name='${key}']`);
                    if (select) {
                        select.value = json[key];
                    }
                }
            });
        });
    }

    /**
     * Submits creating a task
     */
    function createTask() {
        const form = document.forms.namedItem("task");
        if (form.checkValidity()) {
            const payload = createPayload(form, true);
            fetch("api/task/", {
                method: "POST", headers: {
                    "Content-Type": "application/json"
                }, body: payload
            }).then(response => {
                if (response.status === 201) {
                    loadTasks();
                } else {
                    response.text().then(t => {
                        error(`Failed to create task "${JSON.parse(payload).summary}". Reason: ${t}`);
                    });
                }
            }).catch(reason => {
                console.error(reason);
            });
        }
    }

    /**
     * Submits editing a task
     */
    function editTask() {
        const form = document.forms.namedItem("task");
        const payload = createPayload(form);
        if (form.checkValidity()) {
            fetch("api/task/", {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: payload
            }).then(response => {
                if (response.status === 201 || response.status === 204) {
                    loadTasks();
                } else {
                    response.text().then(t => {
                        error(`Failed to edit task "${JSON.parse(payload).summary}". Reason: ${t}`);
                    });
                }
            }).catch(reason => {
                console.error(reason);
            });
        }
    }

    /**
     * Submits updating a task
     */
    function completeTask(json) {
        const payload = JSON.stringify(json);
        fetch("api/task/" + json.id, {
            method: "PATCH",
            headers: {
                "Content-Type": "application/merge-patch+json"
            },
            body: payload
        }).then(response => {
            if (response.status === 200) {
                loadTasks();
            } else {
                response.text().then(t => {
                    error(`Failed to create task ${json.id}. Reason: ${t}`);
                });
            }
        }).catch(reason => {
            console.error(reason);
        });
    }

    /**
     * Creates the payload for the REST call.
     * @param form the form to extract the data from
     * @param skipId skips the ID attribute in the payload
     * @returns {string} the form in JSON format
     */
    function createPayload(form, skipId = false) {
        const formData = new FormData(form);
        let json = {};
        formData.forEach((value, key) => {
            if (!(skipId && key === "id")) {
                json[key] = value;
            }
        });
        return JSON.stringify(json);
    }

    function resetForm(form) {
        form.reset();
        // Reset the text content
        form.querySelectorAll("textarea").forEach((a) => {
            a.textContent = "";
        });
    }

    function makeDraggable(modal) {
        if (!modal) return;

        const modalHeader = modal.querySelector(".modal-header");
        if (!modalHeader) return;

        modalHeader.onmousedown = (e) => {
            e.preventDefault(); // Prevent text selection during drag

            const offsetX = e.clientX - modal.getBoundingClientRect().left;
            const offsetY = e.clientY - modal.getBoundingClientRect().top;

            modal.style.position = "absolute"; // Set position once

            let requestId = null;
            let latestMouseX = e.clientX;
            let latestMouseY = e.clientY;

            const mouseMoveHandler = (e) => {
                latestMouseX = e.clientX;
                latestMouseY = e.clientY;

                if (!requestId) {
                    requestId = requestAnimationFrame(() => {
                        modal.style.left = (latestMouseX - offsetX) + "px";
                        modal.style.top = (latestMouseY - offsetY) + "px";
                        requestId = null;
                    });
                }
            }

            const mouseUpHandler = () => {
                if (requestId) {
                    cancelAnimationFrame(requestId);
                }
                document.removeEventListener("mousemove", mouseMoveHandler);
                document.removeEventListener("mouseup", mouseUpHandler);
            }

            document.addEventListener("mousemove", mouseMoveHandler);
            document.addEventListener("mouseup", mouseUpHandler);
        };
    }

    function success(message) {
        showAlert(message);
    }

    function error(message) {
        showAlert(message, "danger", "false");
    }

    function showAlert(message, type = "success", autoHide = "true") {
        const alertPlaceholder = document.querySelector("#liveAlertPlaceholder");
        const alert = document.querySelector("#alert").content.cloneNode(true).querySelector("div.toast");
        alert.setAttribute("data-bs-autohide", autoHide);
        alert.classList.add("text-bg-" + type);
        const body = alert.querySelector(".toast-body");
        body.textContent = message;
        alert.addEventListener("hidden.bs.toast", () => {
            alertPlaceholder.removeChild(alert);
        });
        const toast = new bootstrap.Toast(alert);
        toast.show();
        alertPlaceholder.append(alert);
    }

    function enableToolTips() {
        const tooltips = document.querySelectorAll('[data-bs-toggle="tooltip"]');
        tooltips.forEach((tooltip) => {
            bootstrap.Tooltip.getOrCreateInstance(tooltip, {
                "delay": {
                    "show": 500
                }
            });
        });
    }
})()