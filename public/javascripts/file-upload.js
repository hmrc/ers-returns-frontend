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

function ensureUploadButtonIs(status) {
       if (status === "enabled") {
        if(uploadFileButton.attributes.getNamedItem("disabled") !== null) {
            uploadFileButton.removeAttribute("disabled");
            uploadFileButton.classList.remove("govuk-button--disabled");
       }
    }
}

// File checks
function checkFileName() {
    let actualFileName = fileUploadInput.files[0].name;
    let lowerName = actualFileName.toLowerCase();
        if (fileType.toLowerCase() === "csv".toLowerCase()) {
            let expectedFileName = actualFileName === document.getElementsByTagName('label').item(0).getAttribute('data-file-name');
            return {fileName: lowerName.endsWith(".csv") && expectedFileName, fileNameLength: true, fileNameCharacters: true};
        } else if (fileType.toLowerCase() === "ods".toLowerCase()) {
            return {fileName: lowerName.endsWith(".ods"), fileNameLength: actualFileName.length < 240, fileNameCharacters: actualFileName.match("[/^~\"|#?,\\]\\[£$&:@*\\\\+%{}<>\\/]|]") == null};
    } else {
        return {fileName: false, fileNameLength: false, fileNameCharacters: false};
    }
}

function checkFileSize() {
    let actualFileSize = fileUploadInput.files[0].size;
    let maxFileSize = (function() { if(fileType === "csv") {return 200000000;} else if(fileType === "ods") {return 200000000;} })();
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
        if (fileChecks.validFileSize === true) {
            ensureUploadButtonIs("enabled");
            makeErrorSummary("disappear");
            clearFileUploadErrorMessage();
        } else {
            makeErrorSummary("appear");
            addErrorToSummary("ers.file.upload."+fileType+".file.large");
            addErrorMessageToFileUpload("ers.file.upload."+fileType+".file.large");
        }
    } else if (fileChecks.validFileName.fileName === false) {
        makeErrorSummary("appear");
        addErrorToSummary("ers.file.upload.wrong.file");
        addErrorMessageToFileUpload("ers.file.upload.wrong.file");
    } else if (fileChecks.validFileName.fileNameLength === false) {
        makeErrorSummary("appear");
        addErrorToSummary("ers.file.upload.ods.too.long");
        addErrorMessageToFileUpload("ers.file.upload.ods.too.long");
    } else if (fileChecks.validFileName.fileNameCharacters === false) {
        makeErrorSummary("appear");
        addErrorToSummary("ers.file.upload.ods.invalid.characters");
        addErrorMessageToFileUpload("ers.file.upload.ods.invalid.characters");
    }
}

// Event listeners
fileUploadInput.addEventListener('input', () => {
    const fileChecks = {validFileName: checkFileName(), validFileSize: checkFileSize()}
    handleErrors(fileChecks);
});

uploadFileButton.addEventListener('click', (e) => {
    let actualFileName = fileUploadInput.files[0];
    if(!actualFileName) {
        e.preventDefault()
        makeErrorSummary("appear");
        addErrorToSummary("ers.file.upload.empty.error");
        addErrorMessageToFileUpload("ers.file.upload.empty.error");
        return false
    }

    //Prevent submitting the page if there is popup error on page
    let errorSummaries = document.getElementsByClassName('govuk-error-summary');
    if (errorSummaries.length > 0) {
        let errorSummary = errorSummaries[0];
        if(errorSummary.className != "govuk-error-summary govuk-!-display-none"){
            e.preventDefault()
            return false
        }
    }

    document.getElementById('progress-spinner').classList.remove('govuk-!-display-none');
    document.getElementById('warning-text').setAttribute("role", "alert")
});
