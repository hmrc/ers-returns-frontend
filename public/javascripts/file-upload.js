const errorSummary = document.getElementsByClassName('govuk-error-summary');
const errorSummaryList = document.getElementsByClassName('govuk-error-summary__list');
const fileUploadInput = document.getElementById('fileToUpload');
const fileUploadDivWrapper = fileUploadInput.parentElement;
const uploadFileButton = document.getElementById('submit');
const fileType = (function() {
    if (document.URL.includes("csv")) {
        return "csv";
    } else if (document.URL.includes("ods")) {
        return "ods";
    } else {
        return "";
    }
})();

// Only show file-upload input if JavaScript is enabled.
fileUploadInput.classList.remove("govuk-!-display-none");

function ensureUploadButtonIs(status) {
    if(status === "disabled") {
        if(uploadFileButton.attributes.getNamedItem("disabled") === null) {
            uploadFileButton.setAttribute("disabled", "disabled");
            uploadFileButton.classList.add("govuk-button--disabled");
        }
    } else if (status === "enabled") {
        if(uploadFileButton.attributes.getNamedItem("disabled") !== null) {
            uploadFileButton.removeAttribute("disabled");
            uploadFileButton.classList.remove("govuk-button--disabled");
        }
    }
}

// When page is refreshed, input becomes empty and therefore submit button should be disabled.
ensureUploadButtonIs("disabled");

// File checks
function checkFileName() {
    let actualFileName = fileUploadInput.files[0].name;
    if (fileType === "csv") {
        let expectedFileName = document.getElementsByTagName('label').item(0).getAttribute('data-file-name');
        return {fileName: actualFileName === expectedFileName, fileNameLength: true, fileNameCharacters: true};
    } else {
        return {fileName: true, fileNameLength: actualFileName.length < 240, fileNameCharacters: actualFileName.match("[/^~\"|#?,\\]\\[£$&:@*\\\\+%{}<>\\/]|]") == null};
    }
}

function checkFileType() {
    let expectedFileType = (function() {
        if(fileType === "csv") {return "text/csv";} else if(fileType === "ods") {return "application/vnd.oasis.opendocument.spreadsheet";}
    })();
    let actualFileType = fileUploadInput.files[0].type;
    return actualFileType === expectedFileType;
}

function checkFileSize() {
    let actualFileSize = fileUploadInput.files[0].size;
    let maxFileSize = (function() { if(fileType === "csv") {return 100000000;} else if(fileType === "ods") {return 10000000;} })();
    return actualFileSize < maxFileSize;
}

// Error summary handling
function clearErrorSummary() {
    errorSummaryList[0].innerHTML = "";
}

function makeErrorSummary(status) {
    if(status === "appear") {
        errorSummary[0].classList.remove("govuk-!-display-none");
    } else if (status === "disappear") {
        errorSummary[0].classList.add("govuk-!-display-none");
        clearErrorSummary();
    }
}

function addErrorToSummary(errorType) {
    clearErrorSummary();
    let newErrorMessage = getLocalisedContent(errorType);
    let newErrorLink = document.createElement("li");
    newErrorLink.innerHTML = "<a href='#fileToUpload'>"+ newErrorMessage +"</a>";
    errorSummaryList[0].appendChild(newErrorLink);
}

function addErrorMessageToFileUpload(errorType) {
    let newErrorMessage = getLocalisedContent(errorType);
    clearFileUploadErrorMessage();
    fileUploadInput.classList.add("govuk-file-upload--error");
    fileUploadInput.setAttribute("described-by", "fileToUpload-error");
    fileUploadDivWrapper.classList.add("govuk-form-group--error");
    let innerErrorSpan = document.createElement('span');
    innerErrorSpan.className = "govuk-visually-hidden";
    innerErrorSpan.innerText = "Error:";
    let errorSpan = document.createElement('span');
    errorSpan.id = "fileToUpload-error";
    errorSpan.className = "govuk-error-message";
    errorSpan.innerText = newErrorMessage;
    errorSpan.appendChild(innerErrorSpan);
    fileUploadInput.insertAdjacentElement('beforebegin', errorSpan);
}

function clearFileUploadErrorMessage() {
    fileUploadInput.classList.remove("govuk-file-upload--error");
    fileUploadInput.removeAttribute("described-by");
    fileUploadDivWrapper.classList.remove("govuk-form-group--error");
    Array.from(fileUploadDivWrapper.children).map((child, index, array) => {
        if(child.tagName === "SPAN") {array[index].remove()}
    });
}

function handleErrors(fileChecks) {
    if(fileChecks.validFileName.fileName === true && fileChecks.validFileName.fileNameLength === true && fileChecks.validFileName.fileNameCharacters === true) {
        if (fileChecks.validFileType === true) {
            if (fileChecks.validFileSize === true) {
                ensureUploadButtonIs("enabled");
                makeErrorSummary("disappear");
                clearFileUploadErrorMessage();
            } else {
                ensureUploadButtonIs("disabled");
                makeErrorSummary("appear");
                addErrorToSummary("ers.file.upload."+fileType+".file.large");
                addErrorMessageToFileUpload("ers.file.upload."+fileType+".file.large");
            }
        } else {
            ensureUploadButtonIs("disabled");
            makeErrorSummary("appear");
            addErrorToSummary("ers.file.upload."+fileType+".wrong.type");
            addErrorMessageToFileUpload("ers.file.upload."+fileType+".wrong.type");
        }
    } else if (fileChecks.validFileName.fileName === false) {
        ensureUploadButtonIs("disabled");
        makeErrorSummary("appear");
        addErrorToSummary("ers.file.upload.wrong.file");
        addErrorMessageToFileUpload("ers.file.upload.wrong.file");
    } else if (fileChecks.validFileName.fileNameLength === false) {
        ensureUploadButtonIs("disabled");
        makeErrorSummary("appear");
        addErrorToSummary("ers.file.upload.ods.too.long");
        addErrorMessageToFileUpload("ers.file.upload.ods.too.long");
    } else if (fileChecks.validFileName.fileNameCharacters === false) {
        ensureUploadButtonIs("disabled");
        makeErrorSummary("appear");
        addErrorToSummary("ers.file.upload.ods.invalid.characters");
        addErrorMessageToFileUpload("ers.file.upload.ods.invalid.characters");
    }
}

// Event listeners
fileUploadInput.addEventListener('input', () => {
    const fileChecks = {validFileName: checkFileName(), validFileType: checkFileType(), validFileSize: checkFileSize()}
    handleErrors(fileChecks);
});

uploadFileButton.addEventListener('click', () => {
    document.getElementById('progress-spinner').classList.remove('govuk-!-display-none');
});
