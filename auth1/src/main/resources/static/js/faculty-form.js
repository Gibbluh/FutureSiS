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

    // Remove the inline onclick handler and add the event listener here
    dropdownToggle.addEventListener('click', function(e) {
        e.preventDefault();
        e.stopPropagation();
        dropdownList.classList.toggle('show');
        dropdownToggle.classList.toggle('show');
        console.log('Toggled dropdown:', dropdownList.classList.contains('show'));
    });

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
        
        console.log('Loading subjects for programs:', selectedPrograms);
        
        if (selectedPrograms.length === 0) {
            subjectAssignments.innerHTML = '<div class="no-subjects-message">Please select departments to view available subjects</div>';
            return;
        }

        // Get CSRF token
        const token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

        console.log('CSRF Token:', token ? 'Found' : 'Not found');
        console.log('CSRF Header:', header ? 'Found' : 'Not found');

        console.log('Making fetch request to load subjects...');
        
        // Show loading state
        subjectAssignments.innerHTML = '<div class="loading-subjects">Loading subjects</div>';
        
        // Use the correct URL with the full path
        const url = `/admin/faculty/subjects/by-programs?programIds=${selectedPrograms.join(',')}`;
        console.log('Request URL:', url);
        
        fetch(url, {
            method: 'GET',
            headers: {
                'Accept': 'application/json',
                [header]: token
            },
            credentials: 'same-origin'
        })
        .then(response => {
            console.log('Response status:', response.status);
            console.log('Response headers:', [...response.headers.entries()]);
            if (!response.ok) {
                return response.text().then(text => {
                    throw new Error(`HTTP error! status: ${response.status}, body: ${text}`);
                });
            }
            return response.json();
        })
        .then(subjects => {
            console.log('Received subjects:', subjects);
            
            if (!Array.isArray(subjects)) {
                console.error('Received non-array response:', subjects);
                throw new Error('Invalid response format');
            }
            
            if (subjects.length === 0) {
                subjectAssignments.innerHTML = '<div class="no-subjects-message">No subjects found for the selected departments</div>';
                return;
            }
            
            // Group subjects by program
            const subjectsByProgram = {};
            subjects.forEach(subject => {
                if (subject && subject.course && subject.course.program) {
                    const programId = subject.course.program.id.toString();
                    if (!subjectsByProgram[programId]) {
                        subjectsByProgram[programId] = [];
                    }
                    subjectsByProgram[programId].push(subject);
                } else {
                    console.warn('Invalid subject data:', subject);
                }
            });

            console.log('Grouped subjects:', subjectsByProgram);

            // Create HTML for each program's subjects
            let html = '<div class="program-subjects-wrapper">';
            selectedPrograms.forEach(programId => {
                const programSubjects = subjectsByProgram[programId] || [];
                console.log(`Creating HTML for program ${programId}, found ${programSubjects.length} subjects`);
                
                if (programSubjects.length > 0) {
                    html += `
                        <div class="program-subjects-section">
                            <h4>${programNames[programId]}</h4>
                            <div class="subject-grid">
                    `;
                    programSubjects.forEach(subject => {
                        html += `
                            <div class="subject-item">
                                <label>
                                    <input type="checkbox" 
                                           name="subjectIds" 
                                           value="${subject.id}">
                                    <span>${subject.code} - ${subject.name}</span>
                                </label>
                            </div>
                        `;
                    });
                    html += '</div></div>';
                } else {
                    html += `
                        <div class="program-subjects-section">
                            <h4>${programNames[programId]}</h4>
                            <div class="no-subjects-message">No subjects found for this department</div>
                        </div>
                    `;
                }
            });
            html += '</div>';
            
            console.log('Setting innerHTML for subject assignments');
            subjectAssignments.innerHTML = html;
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
});