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
  var monthIndex = new Date().getMonth(); // 0 = styczen ... 11 = grudzien
  var row = 4 + monthIndex; // wiersz danych miesiaca w zakladce Podsumowanie

  var monthName = podsumowanie.getRange(row, 1).getValue();
  var hours = podsumowanie.getRange(row, 2).getValue();
  var gross = podsumowanie.getRange(row, 3).getValue();
  var net = podsumowanie.getRange(row, 4).getValue();

  return jsonOutput({
    month: monthName || MONTHS_PL[monthIndex],
    year: year,
    hours: hours,
    gross: gross,
    net: net
  });
}

function jsonOutput(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
