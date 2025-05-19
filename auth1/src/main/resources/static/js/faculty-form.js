document.addEventListener('DOMContentLoaded', function() {
    const programSelect = document.querySelector('.multiselect-dropdown');
    const dropdownToggle = document.querySelector('.multiselect-dropdown-toggle');
    const dropdownList = document.querySelector('.multiselect-dropdown-list');
    const dropdownSpan = dropdownToggle.querySelector('span');
    const subjectAssignments = document.getElementById('subjectAssignments');
    let programNames = {};

    console.log('Initializing faculty form...');

    // Initialize program names map
    document.querySelectorAll('.multiselect-dropdown-option label').forEach(label => {
        const checkbox = label.querySelector('input[type="checkbox"]');
        if (checkbox) {
            programNames[checkbox.value] = label.textContent.trim();
            console.log('Mapped program:', checkbox.value, '->', label.textContent.trim());
        }
    });

    console.log('Initialized program names:', programNames);

    // Initialize dropdown toggle text
    if (dropdownSpan) {
        dropdownSpan.textContent = 'Select departments';
        dropdownToggle.classList.add('placeholder');
    } else {
        console.error('Dropdown toggle span not found!');
    }

    // Initialize subject assignments area
    if (subjectAssignments) {
        subjectAssignments.innerHTML = '<div class="no-subjects-message">Please select departments to view available subjects</div>';
    } else {
        console.error('Subject assignments container not found!');
    }

    // Toggle dropdown functionality
    if (dropdownToggle) {
        dropdownToggle.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            dropdownList.classList.toggle('show');
            dropdownToggle.classList.toggle('show');
        });
    }

    // Close dropdown when clicking outside
    document.addEventListener('click', function(e) {
        if (!programSelect.contains(e.target)) {
            dropdownList.classList.remove('show');
            dropdownToggle.classList.remove('show');
        }
    });

    // Handle checkbox changes
    document.querySelectorAll('.multiselect-dropdown-option input[type="checkbox"]').forEach(checkbox => {
        checkbox.addEventListener('change', function(e) {
            console.log('Checkbox changed:', this.value, this.checked);
            updateSelectedPrograms();
            loadSubjects();
        });
    });

    function updateSelectedPrograms() {
        if (!dropdownSpan) {
            console.error('Dropdown toggle span not found in updateSelectedPrograms');
            return;
        }

        const selectedCheckboxes = programSelect.querySelectorAll('input[type="checkbox"]:checked');
        const selectedNames = Array.from(selectedCheckboxes).map(cb => programNames[cb.value]);
        
        console.log('Selected programs:', selectedNames);
        
        if (selectedNames.length === 0) {
            dropdownSpan.textContent = 'Select departments';
            dropdownToggle.classList.add('placeholder');
        } else {
            dropdownSpan.textContent = selectedNames.join(', ');
            dropdownToggle.classList.remove('placeholder');
        }
    }

    function loadSubjects() {
        if (!subjectAssignments) {
            console.error('Subject assignments container not found in loadSubjects');
            return;
        }

        const selectedPrograms = Array.from(
            programSelect.querySelectorAll('input[type="checkbox"]:checked')
        ).map(checkbox => checkbox.value);
        
        const yearLevel = document.getElementById('facultyYearLevel').value;
        const semester = document.getElementById('facultySemester').value;
        
        console.log('Loading subjects with params:', {
            programs: selectedPrograms,
            yearLevel,
            semester
        });
        
        if (selectedPrograms.length === 0 || !yearLevel || !semester) {
            subjectAssignments.innerHTML = '<div class="no-subjects-message">Please select departments, year level, and semester to view available subjects</div>';
            return;
        }

        // Show loading state
        subjectAssignments.innerHTML = '<div class="loading-subjects">Loading subjects</div>';
        
        // Build the URL with all parameters
        const params = new URLSearchParams({
            departments: selectedPrograms.join(','),
            yearLevel: yearLevel,
            semester: semester
        });
        
        fetch(`/admin/subjects/available?${params.toString()}`, {
            headers: {
                'Accept': 'application/json',
                [document.querySelector('meta[name="_csrf_header"]').content]: 
                document.querySelector('meta[name="_csrf"]').content
            }
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('Received data:', data);
            
            if (!data.subjectsByProgram || Object.keys(data.subjectsByProgram).length === 0) {
                subjectAssignments.innerHTML = '<div class="no-subjects-message">No subjects available for the selected criteria</div>';
                return;
            }

            let html = '<div class="program-subjects-container">';
            
            // Iterate through each selected program
            selectedPrograms.forEach(programId => {
                const programSubjects = data.subjectsByProgram[programId] || [];
                const programName = programNames[programId];
                
                if (programSubjects.length > 0) {
                    html += `
                        <div class="program-subjects-section">
                            <h4>${programName}</h4>
                            <div class="subject-grid">
                    `;
                    
                    programSubjects.forEach(subject => {
                        html += `
                            <div class="subject-item">
                                <label>
                                    <input type="checkbox" 
                                           name="subjectIds" 
                                           value="${subject.id}"
                                           data-program-id="${subject.programId}"
                                           data-year-level="${yearLevel}"
                                           data-semester="${semester}">
                                    <span>${subject.code} - ${subject.name}</span>
                                </label>
                            </div>
                        `;
                    });
                    
                    html += '</div></div>';
                }
            });
            
            if (html === '<div class="program-subjects-container">') {
                html = '<div class="no-subjects-message">No subjects found for the selected criteria</div>';
            } else {
                html += '</div>';
            }
            
            subjectAssignments.innerHTML = html;

            // Add change event listeners to checkboxes
            subjectAssignments.querySelectorAll('input[type="checkbox"]').forEach(checkbox => {
                checkbox.addEventListener('change', function() {
                    const subjectId = this.value;
                    const programId = this.getAttribute('data-program-id');
                    const yearLevel = this.getAttribute('data-year-level');
                    const semester = this.getAttribute('data-semester');
                    
                    console.log('Subject selection changed:', {
                        subjectId,
                        programId,
                        yearLevel,
                        semester,
                        checked: this.checked
                    });
                });
            });
        })
        .catch(error => {
            console.error('Error loading subjects:', error);
            subjectAssignments.innerHTML = `
                <div class="no-subjects-message" style="color: #dc3545;">
                    Error loading subjects. Please try again.<br>
                    Error details: ${error.message}
                </div>`;
        });
    }

    // Add event listeners for year level and semester changes
    const yearLevelSelect = document.getElementById('facultyYearLevel');
    const semesterSelect = document.getElementById('facultySemester');
    
    if (yearLevelSelect) {
        yearLevelSelect.addEventListener('change', loadSubjects);
    }
    
    if (semesterSelect) {
        semesterSelect.addEventListener('change', loadSubjects);
    }

    // Add event listeners for department selection changes
    programSelect.querySelectorAll('input[type="checkbox"]').forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            updateSelectedDepartments();
            loadSubjects();
        });
    });

    function updateSelectedDepartments() {
        const selectedDepartments = Array.from(
            programSelect.querySelectorAll('input[type="checkbox"]:checked')
        ).map(cb => programNames[cb.value]);

        if (selectedDepartments.length === 0) {
            dropdownSpan.textContent = 'Select departments';
            dropdownToggle.classList.add('placeholder');
            dropdownToggle.classList.remove('has-selections');
        } else {
            dropdownSpan.textContent = selectedDepartments.join(', ');
            dropdownToggle.classList.remove('placeholder');
            dropdownToggle.classList.add('has-selections');
        }
    }

    // Initial update of selected departments
    updateSelectedDepartments();

    // Add form submission handler
    const editForm = document.getElementById('editFacultyForm');
    if (editForm) {
        editForm.addEventListener('submit', function(e) {
            e.preventDefault();
            console.log('Submitting edit form');
            
            const formData = new FormData(this);
            const facultyId = document.getElementById('editFacultyId').value;

            // Get all selected subjects with their metadata
            const selectedSubjects = [];
            document.querySelectorAll('#editSubjectAssignments input[type="checkbox"]:checked, #subjectAssignments input[type="checkbox"]:checked').forEach(checkbox => {
                selectedSubjects.push({
                    id: parseInt(checkbox.value),
                    yearLevel: parseInt(checkbox.getAttribute('data-year-level')),
                    semester: parseInt(checkbox.getAttribute('data-semester'))
                });
            });

            // Add subject assignments to form data
            formData.append('subjectAssignments', JSON.stringify(selectedSubjects));

            // Get all program IDs
            const programIds = Array.from(document.querySelectorAll('input[name="programIds"]:checked'))
                .map(cb => cb.value);
            
            // Remove existing programIds from FormData and add as array
            formData.delete('programIds');
            programIds.forEach(id => formData.append('programIds', id));
            
            // Add CSRF token
            const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
            formData.append('_csrf', csrfToken);
            
            console.log('Submitting with faculty ID:', facultyId);
            console.log('Selected programs:', programIds);
            console.log('Selected subjects:', selectedSubjects);
            
            fetch(`/admin/faculty/update-admin/${facultyId}`, {
                method: 'POST',
                body: formData,
                headers: {
                    [document.querySelector('meta[name="_csrf_header"]').content]: csrfToken
                }
            })
            .then(response => {
                if (!response.ok) {
                    return response.text().then(text => {
                        try {
                            const jsonError = JSON.parse(text);
                            throw new Error(jsonError.error || 'Error updating faculty member');
                        } catch (e) {
                            if (text.includes('500')) {
                                throw new Error('Server error occurred while updating faculty member');
                            } else {
                                throw new Error('Error updating faculty member');
                            }
                        }
                    });
                }
                return response.json();
            })
            .then(result => {
                console.log('Update result:', result);
                if (result.success) {
                    // Show success message
                    const successMessage = document.createElement('div');
                    successMessage.className = 'message success';
                    successMessage.textContent = result.message || 'Faculty member updated successfully';
                    document.querySelector('.modal-body').insertBefore(successMessage, document.querySelector('.modal-body form'));
                    
                    // Close modal after a short delay and refresh the page
                    setTimeout(() => {
                        closeEditModal();
                        window.location.reload();
                    }, 1500);
                } else {
                    throw new Error(result.message || 'Error updating faculty member');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                // Show error message in modal
                const errorMessage = document.createElement('div');
                errorMessage.className = 'message error';
                errorMessage.textContent = error.message || 'Error updating faculty member';
                document.querySelector('.modal-body').insertBefore(errorMessage, document.querySelector('.modal-body form'));
                
                // Remove error message after 5 seconds
                setTimeout(() => {
                    errorMessage.remove();
                }, 5000);
            });
        });
    }
});