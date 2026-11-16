// Wklej ten kod w: Rozszerzenia > Apps Script (w otwartym arkuszu Google).
// Zmien ponizszy token na wlasny, dlugi i losowy ciag znakow.
var SECRET_TOKEN = "zmien-mnie-na-wlasny-sekret-123";

var MONTHS_PL = [
  "Styczeń", "Luty", "Marzec", "Kwiecień", "Maj", "Czerwiec",
  "Lipiec", "Sierpień", "Wrzesień", "Październik", "Listopad", "Grudzień"
];

function doGet(e) {
  var token = e.parameter.token;
  if (token !== SECRET_TOKEN) {
    return jsonOutput({ error: "Nieprawidlowy token" });
  }

  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var podsumowanie = ss.getSheetByName("Podsumowanie");
  var ustawienia = ss.getSheetByName("Ustawienia");
  if (!podsumowanie || !ustawienia) {
    return jsonOutput({ error: "Nie znaleziono zakladki Podsumowanie/Ustawienia" });
  }

  var year = ustawienia.getRange("B5").getValue();
  var mode = e.parameter.mode || "current";

  if (mode === "all") {
    return fetchAllMonths(ss, podsumowanie, year);
  }

  var today = new Date();
  var monthIndex = today.getMonth();
  var row = 4 + monthIndex;

  var monthName = podsumowanie.getRange(row, 1).getValue();
  var hours = podsumowanie.getRange(row, 2).getValue();
  var gross = podsumowanie.getRange(row, 3).getValue();
  var net = podsumowanie.getRange(row, 4).getValue();

  var editUrl = buildEditUrl(ss, monthName || MONTHS_PL[monthIndex], today.getDate());

  return jsonOutput({
    month: monthName || MONTHS_PL[monthIndex],
    year: year,
    hours: hours,
    gross: gross,
    net: net,
    editUrl: editUrl
  });
}

function fetchAllMonths(ss, podsumowanie, year) {
  var months = [];
  for (var i = 0; i < 12; i++) {
    var row = 4 + i;
    var monthName = podsumowanie.getRange(row, 1).getValue();
    var hours = podsumowanie.getRange(row, 2).getValue();
    var gross = podsumowanie.getRange(row, 3).getValue();
    var net = podsumowanie.getRange(row, 4).getValue();
    months.push({
      month: monthName || MONTHS_PL[i],
      hours: hours || 0,
      gross: gross || 0,
      net: net || 0
    });
  }
  return jsonOutput({ year: year, months: months });
}

function buildEditUrl(ss, sheetName, dayOfMonth) {
  try {
    var sheet = ss.getSheetByName(sheetName);
    if (!sheet) return "";
    var cellRow = dayOfMonth + 2;
    return ss.getUrl() + "#gid=" + sheet.getSheetId() + "&range=B" + cellRow;
  } catch (err) {
    return "";
  }
}

function jsonOutput(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
