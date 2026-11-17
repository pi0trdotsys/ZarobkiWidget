// Wklej ten kod w: Rozszerzenia > Apps Script (w otwartym arkuszu Google).
// Zmien ponizszy token na wlasny, dlugi i losowy ciag znakow.
var SECRET_TOKEN = "zmien-mnie-na-wlasny-sekret-123";

var MONTHS_PL = [
  "Styczeń", "Luty", "Marzec", "Kwiecień", "Maj", "Czerwiec",
  "Lipiec", "Sierpień", "Wrzesień", "Październik", "Listopad", "Grudzień"
];

// Arkusze miesieczne: dzien 1 w wierszu 4, godziny w kolumnie D.
var FIRST_DAY_ROW = 4;
var HOURS_COL = 4;
var HOURS_COL_LETTER = "D";

function doGet(e) {
  var p = e.parameter;
  if (p.token !== SECRET_TOKEN) {
    return jsonOutput({ error: "Nieprawidlowy token" });
  }

  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var podsumowanie = ss.getSheetByName("Podsumowanie");
  var ustawienia = ss.getSheetByName("Ustawienia");
  if (!podsumowanie || !ustawienia) {
    return jsonOutput({ error: "Nie znaleziono zakladki Podsumowanie/Ustawienia" });
  }

  var year = ustawienia.getRange("B5").getValue();
  var mode = p.mode || "current";

  if (mode === "all") return fetchAllMonths(podsumowanie, year);
  if (mode === "day") return dayInfo(ss, year, p.date);
  if (mode === "set") return setHours(ss, podsumowanie, year, p.date, p.hours);

  return jsonOutput(monthSummary(ss, podsumowanie, year, new Date()));
}

function monthSummary(ss, podsumowanie, year, date) {
  var monthIndex = date.getMonth();
  var values = podsumowanie.getRange(4 + monthIndex, 1, 1, 4).getValues()[0];
  return {
    month: values[0] || MONTHS_PL[monthIndex],
    year: year,
    hours: values[1] || 0,
    gross: values[2] || 0,
    net: values[3] || 0,
    editUrl: buildEditUrl(ss, monthIndex, date.getDate())
  };
}

function fetchAllMonths(podsumowanie, year) {
  var rows = podsumowanie.getRange(4, 1, 12, 4).getValues();
  var months = rows.map(function (r, i) {
    return { month: r[0] || MONTHS_PL[i], hours: r[1] || 0, gross: r[2] || 0, net: r[3] || 0 };
  });
  return jsonOutput({ year: year, months: months });
}

function dayInfo(ss, year, dateStr) {
  var d = parseDate(dateStr, year);
  if (d.error) return jsonOutput(d);
  var sheet = ss.getSheetByName(MONTHS_PL[d.month]);
  if (!sheet) return jsonOutput({ error: "Brak zakladki " + MONTHS_PL[d.month] });
  var value = sheet.getRange(FIRST_DAY_ROW + d.day - 1, HOURS_COL).getValue();
  return jsonOutput({
    date: dateStr,
    hours: Number(value) || 0,
    editUrl: buildEditUrl(ss, d.month, d.day)
  });
}

function setHours(ss, podsumowanie, year, dateStr, hoursStr) {
  var d = parseDate(dateStr, year);
  if (d.error) return jsonOutput(d);
  var hours = parseFloat(String(hoursStr).replace(",", "."));
  if (isNaN(hours) || hours < 0 || hours > 24) {
    return jsonOutput({ error: "Nieprawidlowa liczba godzin" });
  }
  var sheet = ss.getSheetByName(MONTHS_PL[d.month]);
  if (!sheet) return jsonOutput({ error: "Brak zakladki " + MONTHS_PL[d.month] });

  sheet.getRange(FIRST_DAY_ROW + d.day - 1, HOURS_COL).setValue(hours > 0 ? hours : "");
  SpreadsheetApp.flush();

  var result = monthSummary(ss, podsumowanie, year, new Date());
  result.date = dateStr;
  result.dayHours = hours;
  return jsonOutput(result);
}

function parseDate(dateStr, year) {
  var m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(dateStr || "");
  if (!m) return { error: "Nieprawidlowa data" };
  var y = Number(m[1]), month = Number(m[2]) - 1, day = Number(m[3]);
  if (y !== Number(year)) return { error: "Arkusz dotyczy roku " + year };
  var daysInMonth = new Date(y, month + 1, 0).getDate();
  if (month < 0 || month > 11 || day < 1 || day > daysInMonth) {
    return { error: "Nieprawidlowa data" };
  }
  return { month: month, day: day };
}

function buildEditUrl(ss, monthIndex, day) {
  var sheet = ss.getSheetByName(MONTHS_PL[monthIndex]);
  if (!sheet) return "";
  var row = FIRST_DAY_ROW + day - 1;
  return ss.getUrl() + "#gid=" + sheet.getSheetId() + "&range=" + HOURS_COL_LETTER + row;
}

function jsonOutput(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
