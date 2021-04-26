	/*********************/
	/* Global Check file */
	/*********************/

	var MAX_FILENAME_LENGTH = 240
	var MAX_FILESIZE = 10000000  //10Mb
	var errors = 0;	
	
	$("#file-uploader button").attr("disabled",true);
	
	// ----------------------------------------------------------
	// If you're not in IE (or IE version is less than 5) then:
	// ie === undefined
	// If you're in IE (>=5) then you can determine which version:
	// ie === 7; // IE7
	// Thus, to detect IE:
	// if (ie) {}
	// And to detect the version:
	// ie === 6 // IE6
	// ie > 7 // IE8, IE9, IE10 ...
	// ie < 9 // Anything less than IE9
	// ----------------------------------------------------------
	var ie = (function(){
	    var undef,rv = -1; // Return value assumes failure.
	    var ua = window.navigator.userAgent;
	    var msie = ua.indexOf('MSIE ');
	    var trident = ua.indexOf('Trident/');
	    var edge = ua.indexOf('Edge/');
	    if (msie > 0) {
	        // IE 10 or older => return version number
	        rv = parseInt(ua.substring(msie + 5, ua.indexOf('.', msie)), 10);
	    } else if (trident > 0) {
	        // IE 11 (or newer) => return version number
	        var rvNum = ua.indexOf('rv:');
	        rv = parseInt(ua.substring(rvNum + 3, ua.indexOf('.', rvNum)), 10);
	    } else if (edge > 0) {
			// Edge
			rv = 13
        }
	    return ((rv > -1) ? rv : undef);
	}());				
	
	/* Sanatise Filename */
	function validFileName(fileName) {
	    if (fileName.match("[/^~\"|#?,\\]\\[£$&:@*\\\\+%{}<>\\/]|]") == null) {
	        return true;
	    } else {
	        return false;
	    }
	}

	function getFileNameExtension(fileName)
	{
	    return typeof fileName != "undefined" ? fileName.substring(fileName.lastIndexOf(".")+1, fileName.length).toLowerCase() : false;
	}

	function fileSizeOK () {
		if (ie<10) {
			return true
		} else {
			var fileSize = $("#fileToUpload")[0].files[0].size;
			if (fileSize > MAX_FILESIZE) {
				return false
			} else {
				return true
			}
		}		
	}
		
	function removeErrorMsg() {
    	$("#error-summary").remove();
    	$(".validation-summary").hide()
    	$("#file-uploader button").attr("disabled",false).focus();
	    $("#uploadForm").removeClass("form-field--error");
	}

	function showErrorMsg(msg) {
    	if ($("#error-summary").length) {
    		$("#error-summary").remove();
    	}
    	$("#file-uploader button").attr("disabled",true);
	    $("#file-wrapper").before("<div id='error-summary' class='error-notification' tabindex'-1' role='alert' aria-labelledby='error-heading'>"+msg+"</div>")
	}	


	$(document).ready(function () {

		/* Spinner */
		if (ie) {
			$("#file-uploader button").on("click", function (e) {
				var temp = $("#progress-spinner-img").attr("src");
				$("#progress-spinner").show();
				$("#progress-spinner-img").attr("src",temp);
			});
			$("#continue").on("click", function (e) {
				$(".summary-button").attr("disabled",true)
				var temp = $("#progress-spinner-img").attr("src");
				$("#progress-spinner").show();
				$("#progress-spinner-img").attr("src",temp);
			});
		} else {
			$("#file-uploader button").on("click", function (e) {
				$("#progress-spinner").show();
			});
			$("#continue").on("click", function (e) {
				$(".summary-button").attr("disabled",true)
				$("#progress-spinner").show();
			});
		}


		$('.validation-summary-message a').on('click', function(e){
			e.preventDefault();
				var focusId = $(this).attr('data-focuses'),
				thingToFocus = $("#"+focusId);
			$('html, body').animate({
				scrollTop: thingToFocus.parent().offset().top
			}, 500);
			thingToFocus.parent().find('#fileToUpload').first().focus();
			thingToFocus.parent().find('.block-label').first().focus();
			thingToFocus.parent().find('.form-control').first().focus();
		});

		$('#errors').focus();

	});