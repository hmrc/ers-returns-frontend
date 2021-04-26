const ukRadioButton = document.getElementById('uk-radio-button')
const overseasRadioButton = document.getElementById('overseas-radio-button')

if(ukRadioButton.checked) {
    document.getElementById('country').parentElement.classList.add('govuk-visually-hidden')
    selectCountry("UK")
} else if (overseasRadioButton.checked) {
    document.getElementById('postcode').parentElement.classList.add('govuk-visually-hidden')
}

ukRadioButton.addEventListener('input', () => {
    document.getElementById('country').parentElement.classList.add('govuk-visually-hidden')
    document.getElementById('postcode').parentElement.classList.remove('govuk-visually-hidden')
    selectCountry("UK")
})

overseasRadioButton.addEventListener('input', () => {
    document.getElementById('postcode').parentElement.classList.add('govuk-visually-hidden')
    document.getElementById('country').parentElement.classList.remove('govuk-visually-hidden')
    if(document.getElementById('country').value === "UK") {
        selectCountry("")
    }
})

function selectCountry(country) {
    var selectElement = document.getElementById('country');
    var options = selectElement.options;
    for (var opt, i = 0; opt = options[i]; i++) {
        if (opt.value == country) {
            selectElement.selectedIndex = i;
            break;
        }
    }
}
