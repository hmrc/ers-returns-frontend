	/***********************/
	/* Check ODS file page */
	/***********************/

	$("#choose-file-button").click(function (e) {
		$("#input-file-name").click();
	});	

	function showODSErrorMsg(msg) {
    	if ($("#error-summary").length) {
    		$("#error-summary").remove();
    		$(".validation-summary").hide()
	    	$("#uploadForm").removeClass("form-field--error");
    	}
    	$("#file-uploader button").attr("disabled",true);
    	$(".validation-summary").show()
	    $("#fileToUpload").parent("Div").before("<p id='error-summary' class='field-error error-notification' tabindex'-1' role='alert' aria-labelledby='error-heading'>"+msg+"</p>")
	    $(".validation-summary-message a").html(msg)
	    $("#uploadForm").addClass("form-field--error");
	    $("#errors").focus();
	}
	
	
	$("#fileToUpload").change(function(e){					
		var $el = $('#fileToUpload');		
		// extract file name for validation 
		if (ie<10) {			
			var fileName = $el.val().substr($el.val().lastIndexOf("\\")+1, $el.val().length);	
		} else {
			var fileName = $("#fileToUpload")[0].files[0].name;
		}		
		
		// Check file name
		if (validFileName(fileName)) {			
			// check file name length
			if (fileName.length <= MAX_FILENAME_LENGTH) {
				// Check file extn
				if (getFileNameExtension(fileName) == "ods") {
					if (fileSizeOK()) {
						// file ok
				    	removeErrorMsg();						
					} else {
						showODSErrorMsg(GOVUK.getLocalisedContent("ers.file.upload.ods.large"));
					}
				} else {
			    	showODSErrorMsg(GOVUK.getLocalisedContent("ers.file.upload.ods.wrong.type"));
				}
			} else {
		    	showODSErrorMsg(GOVUK.getLocalisedContent("ers.file.upload.ods.too.long"));
		    }
		} else {
	    	showODSErrorMsg(GOVUK.getLocalisedContent("ers.file.upload.ods.invalid.characters"));
		}		
				
		// extract filename for display 
		$("#file-name").text(fileName);	
		
		// show page elements
		$("#file-header-bar").show();
		
		if (ie<11) {
			$("#remove-file-link").insertAfter("#input-file-name")
			$("#file-header-bar").css("padding-left","3px")
		}		
		$("#remove-file-link").show();
	});
	
	
	
	    