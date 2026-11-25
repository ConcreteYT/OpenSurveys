/**
 * Values for the [Token] blanks in legal.js.
 * The same keys are used in the German and English text.
 * legal.js copies each value into both languages.
 * Leave a value as '' while it is unknown. legal.js then keeps the bracket,
 * and the page keeps highlighting it. Do not copy the bracket text into this file.
 */
export const legalPlaceholders = {
  // Site operator. Your name, or the school's name if the school runs the site. Not the host.
  'Name': '',
  // Street and house number of that same person or school.
  'Straße und Hausnummer': '',
  // Postcode and city.
  'PLZ und Ort': '',
  // Country, for example Deutschland.
  'Land': '',
  // Email address where people can reach the operator. Not the verification mailbox unless you choose to publish that address.
  'E-Mail': '',
  // Phone number for quick contact.
  'Telefon': '',
  // Company on the hosting invoice.
  'Hosting-Anbieter': '',
  // Postal address of that company, from its German terms. The VPS itself is in London.
  'Anschrift des Hosting-Anbieters': '',
  // Company that sends the verification codes. The mailbox is mailingtestpolling@gmail.com.
  'E-Mail-Anbieter': '',
  // Data-protection authority of the operator's German state.
  'Zuständige Aufsichtsbehörde': '',
}
