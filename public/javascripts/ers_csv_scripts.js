	/***********************/
	/* Check CSV file page */
	/***********************/

	var MAX_CSV_FILESIZE = 100000000// 100Mb

	function csvFileSizeOK (fileSize) {
		if (ie<10) {
			return true
		} else {
			if (fileSize > MAX_CSV_FILESIZE) {
				return false
			} else {
				return true
			}
		}
	}

	function isValidCSVFile (filename) {
		var matchCount = 0
		$("#fileName").each(function(index){
			if ($(this)[0].getAttribute("data-file-name") == filename) {matchCount++;}
		});
		if (matchCount > 0) {
			return true;
		} else {
			return false;
		}
	}


	function duplicateFileName (fileName) {
		var duplicateNameCount = 0
		$(".files").each(function(index){
			if ($(this).val() != "") {
				if (ie<10) {
					if ($(this).val().substr($(this).val().lastIndexOf("\\")+1, $(this).val().length) == fileName) {
						duplicateNameCount++;
						if (duplicateNameCount > 1) $(this).parent("Div").addClass("fileAlert")
					}
				} else {
					if ($(this)[0].files[0].name == fileName) {
						duplicateNameCount++;
						if (duplicateNameCount > 1) $(this).parent("Div").addClass("fileAlert")
					}
				}
			}
		});
		if (duplicateNameCount > 1) {
			return true;
		} else {
			return false;
		}
	}

	function removeFileAlert () {
		$("#fileToUpload").parent("Div").removeClass("fileAlert");
	}

	function showCSVErrorMsg(e, msg) {
    	if ($("#error-summary").length) {
    		$("#error-summary").remove();
    		$(".validation-summary").hide()
	    	$("#uploadForm").removeClass("form-field--error");
    	}
    	$("#file-uploader button").attr("disabled",true);
    	$(".validation-summary").show()
	    $("#fileToUpload").parent("Div").before("<p id='error-summary' class='field-error error-notification' tabindex='-1' role='alert'>"+msg+"</p>")
	    $(".validation-summary-message a").html(msg)
	    $("#uploadForm").addClass("form-field--error");
	    $("#errors").focus();
	}

	function validateFile(fileName, fileSize, e) {
        // check file name length
        // Check file extn
        if (getFileNameExtension(fileName) == "csv") {
            if (csvFileSizeOK(fileSize)) {
                if (isValidCSVFile(fileName)) {
                    // file ok
                    return true;
                } else {
                    var filesListMsg = GOVUK.getLocalisedContent("ers.file.upload.wrong.file");
                    showCSVErrorMsg(e, filesListMsg);
                    errors++;
                    return false;
                }
            } else {
                showCSVErrorMsg(e, GOVUK.getLocalisedContent("ers.file.upload.csv.file.large"));
                errors++;
                return false;
            }
        } else {
            showCSVErrorMsg(e, GOVUK.getLocalisedContent("ers.file.upload.csv.wrong.type"));
            errors++;
            return false;
        }
	}

	$("#fileToUpload").change(function(e){
		errors = 0;
		$("#fileToUpload").each(function(index){
			if (ie<10) {
				var fileName = $(this).val().substr($(this).val().lastIndexOf("\\")+1, $(this).val().length);
			} else {
				var fileName = $(this)[0].files[0].name;
				var fileSize = $(this)[0].files[0].size;
			}
			if (fileName != undefined) {
				if (!validateFile(fileName, fileSize, this)) {
					removeFileAlert();
					$(this).parent("Div").addClass("fileAlert");
				}
			}
		});
		if (errors == 0) {
			removeFileAlert();
			removeErrorMsg();
		}
	});


