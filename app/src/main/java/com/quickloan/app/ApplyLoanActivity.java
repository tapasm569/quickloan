// Indian Date Formatting (DD-MM-YYYY)
SimpleDateFormat indianDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
String today = indianDateFormat.format(new Date());

Calendar cal = Calendar.getInstance();
cal.add(Calendar.DAY_OF_YEAR, selectedDays);
String dueDate = indianDateFormat.format(cal.getTime());

map.put("date", today);
map.put("due_date", dueDate);
