var content = {
  "en" : {
    "ers.file.upload.wrong.file" : "This is not a file that you said you needed to upload, choose a different file",
    "ers.file.upload.csv.file.large" : "This file is larger than 200MB – choose a different file or email shareschemes@hmrc.gsi.gov.uk and we will help you submit your return",
    "ers.file.upload.csv.wrong.type" : "This file is not a .csv file, choose a different file",
    "ers.file.upload.ods.file.large" : "This file is larger than 200MB – choose a different file or email shareschemes@hmrc.gsi.gov.uk and we will help you submit your return",
    "ers.file.upload.ods.wrong.type" : "This file is not a .ods file, choose a different file",
    "ers.file.upload.ods.too.long" : "The filename must contain 240 characters or less",
    "ers.file.upload.ods.invalid.characters" : "The filename contains invalid characters",
    "ers.file.upload.empty.error" : "Select a file to upload"
  },
  "cy" : {
     "ers.file.upload.wrong.file" : "Nid yw hon yn ffeil y rhoesoch wybod fod angen i chi ei huwchlwytho – dewiswch ffeil wahanol",
     "ers.file.upload.csv.file.large" : "Mae’r ffeil hon yn fwy na 200MB – dewiswch ffeil wahanol, neu e-bostiwch gwasanaeth.cymraeg@hmrc.gsi.gov.uk a byddwn yn eich helpu i gyflwyno’ch datganiad",
     "ers.file.upload.csv.wrong.type" : "Nid ffeil .csv yw hon – dewiswch ffeil wahanol",
     "ers.file.upload.ods.file.large" : "Mae’r ffeil hon yn fwy na 200MB – dewiswch ffeil wahanol, neu e-bostiwch gwasanaeth.cymraeg@hmrc.gsi.gov.uk a byddwn yn eich helpu i gyflwyno’ch datganiad",
     "ers.file.upload.ods.wrong.type" : "Nid ffeil .ods yw hon – dewiswch ffeil wahanol",
     "ers.file.upload.ods.too.long" : "Rhaid i enw’r ffeil gynnwys 240 o gymeriadau neu lai",
     "ers.file.upload.ods.invalid.characters" : "Mae enw’r ffeil yn cynnwys cymeriadau annilys",
     "ers.file.upload.empty.error" : "Dewiswch ffeil i’w huwchlwytho"
  }
}

const playLanguage = () => {
  var playCookieName = encodeURIComponent("PLAY_LANG") + "=";
  var ca = document.cookie.split(';');
  for (var i = 0; i < ca.length; i++) {
    var c = ca[i];
    while (c.charAt(0) === ' ')
      c = c.substring(1, c.length);
    if (c.indexOf(playCookieName) === 0)
      return decodeURIComponent(c.substring(playCookieName.length, c.length));
  }
  return "en";
}

function getLocalisedContent(key) {
  return content[playLanguage()][key]
}
