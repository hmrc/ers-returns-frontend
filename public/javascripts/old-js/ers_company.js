
$(".country-group").hide();

$("#overseas-radio-button").on('click', function() {
	  $(".country-group").show();
	  $(".postcode-group").hide();
	  $(".company-reg-group").hide();
	  $(".corporation-ref-group").hide();
	  if ($("#country").val() == "UK") {
		  $('option[value="Select..."]').attr('selected', 'selected');
		  $($("#country").val(""));
		  $($("#postcode").val(""));
	  }
});

$("#uk-radio-button").on('click', function() {
	  $(".country-group").hide();
	  $(".postcode-group").show();
	  $(".company-reg-group").show();
	  $(".corporation-ref-group").show();
	  $('option[value="UK"]').attr('selected', 'selected');
	  $($("#country").val("UK"));
});


if ($("#country").val() == "UK" || $("#country").val() == "") {
	$("#uk-radio-button").addClass("selected");
	$("#uk-radio-button").click()
} else {
	$("#overseas-radio-button").click()
}