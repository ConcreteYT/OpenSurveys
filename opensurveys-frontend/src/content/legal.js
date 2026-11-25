/**
 * Impressum and privacy copy. Bracketed tokens are placeholders the operator
 * must replace before the site is offered to other people.
 * The same tokens are used in both languages so one search replaces them everywhere.
 */
export const legalDocuments = {
  de: {
    impressum: {
      title: 'Impressum',
      sections: [
        {
          heading: 'Diensteanbieter',
          paragraphs: [
            'Open Surveys ist ein Schulprojekt zum Erstellen und Ausfüllen von Umfragen (Demo), kein kommerzielles Angebot.',
            'Angaben gemäß § 5 DDG:',
            '[Name]',
            '[Straße und Hausnummer]',
            '[PLZ und Ort]',
            '[Land]',
          ],
        },
        {
          heading: 'Kontakt',
          paragraphs: [
            'E-Mail: [E-Mail]',
            'Telefon: [Telefon]',
          ],
        },
        {
          heading: 'Verantwortlich für den Inhalt',
          paragraphs: [
            'Verantwortlich nach § 18 Abs. 2 MStV: [Name], Anschrift wie oben.',
          ],
        },
        {
          heading: 'Haftung für Inhalte und Links',
          paragraphs: [
            'Die Inhalte dieser Demo wurden sorgfältig erstellt. Für die Richtigkeit, Vollständigkeit und Aktualität wird keine Gewähr übernommen. Bei direkten oder indirekten Verweisen auf fremde Websites (Links) liegt die Verantwortung bei deren Betreibern.',
          ],
        },
      ],
    },
    datenschutz: {
      title: 'Datenschutzerklärung',
      sections: [
        {
          heading: 'Verantwortlicher',
          paragraphs: [
            'Verantwortlich für die Datenverarbeitung auf dieser Website ist:',
            '[Name]',
            '[Straße und Hausnummer]',
            '[PLZ und Ort]',
            '[Land]',
            'E-Mail: [E-Mail]',
          ],
        },
        {
          heading: 'Worum es geht',
          paragraphs: [
            'Open Surveys ist eine Demo aus einem Schulprojekt. Angemeldete Personen können Umfragen anlegen. Andere können eine Umfrage mit einem Zugangscode ausfüllen, ohne ein Konto zu erstellen. Die Anwendung enthält kein Werbe- oder Reichweitenmessungs-Werkzeug.',
            'Rechtsgrundlagen stehen in der Datenschutz-Grundverordnung (DSGVO). Speicherung im Browser, die für den Dienst nötig ist, stützt sich zusätzlich auf § 25 Abs. 2 TDDDG.',
          ],
        },
        {
          heading: 'Hosting und Datenbank',
          paragraphs: [
            'Konten, Umfragen und Antworten liegen in einer MySQL-Datenbank. Wird die Demo im Internet betrieben, verarbeitet der folgende Anbieter die Daten in unserem Auftrag:',
            '[Hosting-Anbieter]',
            '[Anschrift des Hosting-Anbieters]',
            'Läuft die Anwendung nur auf einem einzelnen Rechner, bleiben diese Daten auf diesem Rechner. Rechtsgrundlage ist Art. 6 Abs. 1 lit. f DSGVO (Betrieb der Demo).',
          ],
        },
        {
          heading: 'Konto',
          paragraphs: [
            'Bei der Registrierung speichern wir Name, Benutzername, E-Mail-Adresse, ein Kennzeichen, ob die E-Mail bestätigt wurde, und die Rolle (USER oder ADMIN). Das Passwort wird nur als BCrypt-Hash gespeichert, nie im Klartext. Wer sich mit Google anmeldet, hat möglicherweise kein Passwort; dann speichern wir die Google-Nutzerkennung (sub), um das Konto wiederzuerkennen.',
            'Rechtsgrundlage ist Art. 6 Abs. 1 lit. b DSGVO (Bereitstellung des Kontos). Ein Konto kannst du in den Einstellungen selbst löschen. Dabei werden zuerst die von dir erstellten Umfragen entfernt.',
          ],
        },
        {
          heading: 'Anmeldung und Sitzung',
          paragraphs: [
            'Nach erfolgreicher Anmeldung setzt der Server ein httpOnly-Cookie mit einem JWT. JavaScript im Browser kann dieses Cookie nicht lesen. Im localStorage liegen nur Hinweise für die Oberfläche: ob eine Sitzung besteht, der Benutzername und die Rolle. Die Abmeldung löscht das Cookie und diese Hinweise.',
            'Rechtsgrundlage ist Art. 6 Abs. 1 lit. b DSGVO. Das Cookie ist erforderlich, um die Anmeldung aufrechtzuerhalten.',
          ],
        },
        {
          heading: 'Google-Anmeldung',
          paragraphs: [
            'Die Anmeldung mit Google ist freiwillig. Wenn du sie nutzt, erhält Google die Information, dass du diesen Dienst verwendest, und gibt uns ein ID-Token. Wir prüfen das Token und speichern die darin enthaltene Nutzerkennung, E-Mail und den Namen, soweit Google sie übermittelt.',
            'Hinweise von Google: https://policies.google.com/privacy',
            'Google kann Daten in den USA verarbeiten. Rechtsgrundlage ist Art. 6 Abs. 1 lit. b DSGVO, wenn du das Konto auf diesem Weg anlegst oder nutzt.',
          ],
        },
        {
          heading: 'E-Mail und Verifizierungscodes',
          paragraphs: [
            'Zur Bestätigung der E-Mail-Adresse und für sensible Änderungen (E-Mail, Passwort, Kontolöschung) versenden wir einen Code über:',
            '[E-Mail-Anbieter]',
            'Gespeichert wird nur ein Hash des Codes, zusammen mit Zweck und Ablaufzeit. Nach Verwendung oder Ablauf wird er ungültig. Rechtsgrundlage ist Art. 6 Abs. 1 lit. b und lit. f DSGVO (Kontobereitstellung und Schutz vor Missbrauch).',
          ],
        },
        {
          heading: 'Umfragen und Antworten',
          paragraphs: [
            'Wer eine Umfrage anlegt, speichert Fragetexte, Antwortoptionen und Einstellungen, etwa ob Ergebnisse öffentlich sind und ob nur eine Teilnahme erlaubt ist. Der Benutzername der erstellenden Person kann für Teilnehmende sichtbar sein.',
            'Teilnehmende ohne Konto können Antworten absenden. Was darin steht, legt die jeweilige Umfrage fest. Schreibe keine Daten hinein, die andere nicht sehen sollen, besonders wenn Ergebnisse öffentlich sind.',
            'Der eingegebene Zugangscode wird in der sessionStorage des Browsers gemerkt, damit dieselbe Umfrage in diesem Tab wieder geöffnet werden kann. Sie wird beim Schließen des Tabs verworfen.',
            'Ist nur eine Teilnahme erlaubt, setzt der Server ein httpOnly-Cookie mit dem Namen osr_ und der Umfragenummer. Es gilt etwa zwei Stunden. Auf dem Server liegt nur ein Hash dieses Werts, damit derselbe Browser die Umfrage nicht ein zweites Mal abschickt. Zusätzlich merkt sich der Browser in localStorage, dass diese Umfrage schon abgeschickt wurde.',
            'Rechtsgrundlage ist Art. 6 Abs. 1 lit. b DSGVO für die erstellende Person und Art. 6 Abs. 1 lit. f DSGVO für den Betrieb der Umfrage und den Schutz vor Mehrfachteilnahmen.',
          ],
        },
        {
          heading: 'Darstellung im Browser',
          paragraphs: [
            'Helles oder dunkles Design, Akzentfarbe und Sprache (Deutsch oder Englisch) speichert der Browser in localStorage, damit die Auswahl beim nächsten Besuch erhalten bleibt. Das sind keine Werbecookies. Rechtsgrundlage ist Art. 6 Abs. 1 lit. f DSGVO und § 25 Abs. 2 TDDDG, weil die Speicherung die von dir gewählte Darstellung umsetzt.',
          ],
        },
        {
          heading: 'Administration',
          paragraphs: [
            'Personen mit der Rolle ADMIN können Nutzerkonten einsehen, bearbeiten und löschen, um die Demo zu betreuen. Rechtsgrundlage ist Art. 6 Abs. 1 lit. f DSGVO.',
          ],
        },
        {
          heading: 'Empfänger',
          paragraphs: [
            'Daten erhalten nur die Stellen, die sie für den beschriebenen Zweck brauchen: der [Hosting-Anbieter], soweit die Demo gehostet ist; der [E-Mail-Anbieter] für Verifizierungsmails; Google Ireland bzw. Google, wenn du die Google-Anmeldung nutzt; Administratoren dieser Instanz.',
            'Eine Weitergabe zu Werbezwecken findet nicht statt.',
          ],
        },
        {
          heading: 'Speicherdauer',
          paragraphs: [
            'Kontodaten bleiben, bis du das Konto löschst oder ein Administrator es löscht. Umfragen und Antworten der erstellenden Person werden mit dem Konto entfernt. Verifizierungscodes gelten nur bis zur Verwendung oder bis zum Ablauf. Das Teilnahmecookie gilt etwa zwei Stunden. Einträge in localStorage bleiben, bis du sie im Browser löschst oder dich abmeldest (die Abmeldung entfernt die Sitzungshinweise, nicht Design und Sprache).',
          ],
        },
        {
          heading: 'Deine Rechte',
          paragraphs: [
            'Du hast das Recht auf Auskunft, Berichtigung, Löschung, Einschränkung der Verarbeitung, Datenübertragbarkeit und Widerspruch gegen Verarbeitungen auf Grundlage von Art. 6 Abs. 1 lit. f DSGVO. Eine Einwilligung kannst du mit Wirkung für die Zukunft widerrufen, soweit eine Verarbeitung darauf beruht.',
            'Beschwerde kannst du bei einer Aufsichtsbehörde einlegen. Für diese Demo ist das: [Zuständige Aufsichtsbehörde].',
            'Die Löschung des eigenen Kontos ist in den Einstellungen vorgesehen.',
          ],
        },
        {
          heading: 'Pflicht zur Bereitstellung',
          paragraphs: [
            'Ohne Benutzername, E-Mail und Passwort — oder alternativ eine Google-Anmeldung — kann kein Konto angelegt werden. Eine Umfrage ausfüllen geht ohne Konto. Welche Angaben die Umfrage verlangt, bestimmt die Person, die sie erstellt hat.',
          ],
        },
        {
          heading: 'Automatisierte Entscheidungen',
          paragraphs: [
            'Es gibt keine automatisierte Entscheidungsfindung und kein Profiling im Sinne von Art. 22 DSGVO.',
          ],
        },
      ],
    },
  },
  en: {
    impressum: {
      title: 'Legal notice',
      sections: [
        {
          heading: 'Service provider',
          paragraphs: [
            'Open Surveys is a school project for creating and filling out surveys (a demo), not a commercial service.',
            'Information required by § 5 DDG:',
            '[Name]',
            '[Straße und Hausnummer]',
            '[PLZ und Ort]',
            '[Land]',
          ],
        },
        {
          heading: 'Contact',
          paragraphs: [
            'Email: [E-Mail]',
            'Phone: [Telefon]',
          ],
        },
        {
          heading: 'Responsible for content',
          paragraphs: [
            'Responsible under § 18 (2) MStV: [Name], address as above.',
          ],
        },
        {
          heading: 'Liability for content and links',
          paragraphs: [
            'This demo was prepared with care. No warranty is given for accuracy, completeness, or currency. External sites linked from here are the responsibility of their own operators.',
          ],
        },
      ],
    },
    datenschutz: {
      title: 'Privacy policy',
      sections: [
        {
          heading: 'Controller',
          paragraphs: [
            'The controller for processing on this site is:',
            '[Name]',
            '[Straße und Hausnummer]',
            '[PLZ und Ort]',
            '[Land]',
            'Email: [E-Mail]',
          ],
        },
        {
          heading: 'What this is',
          paragraphs: [
            'Open Surveys is a school-project demo. Signed-in people can create surveys. Others can fill out a survey with an access code and no account. The app has no advertising or audience-measurement tool.',
            'Legal bases are in the GDPR. Storage on the device that the service needs also relies on § 25 (2) TDDDG.',
          ],
        },
        {
          heading: 'Hosting and database',
          paragraphs: [
            'Accounts, surveys, and answers are stored in a MySQL database. If the demo is published on the internet, this provider processes the data on our behalf:',
            '[Hosting-Anbieter]',
            '[Anschrift des Hosting-Anbieters]',
            'If the app runs only on one computer, that data stays on that computer. The legal basis is Art. 6 (1) (f) GDPR (running the demo).',
          ],
        },
        {
          heading: 'Account',
          paragraphs: [
            'On sign-up we store name, username, email address, whether the email was verified, and the role (USER or ADMIN). The password is stored only as a BCrypt hash, never in plain text. A Google sign-in may have no password; we then store the Google subject identifier (sub) so the account can be recognized again.',
            'The legal basis is Art. 6 (1) (b) GDPR (providing the account). You can delete an account in settings. Surveys you created are removed first.',
          ],
        },
        {
          heading: 'Sign-in and session',
          paragraphs: [
            'After a successful sign-in the server sets an httpOnly cookie containing a JWT. Browser JavaScript cannot read that cookie. localStorage only keeps hints for the interface: whether a session exists, the username, and the role. Logging out clears the cookie and those hints.',
            'The legal basis is Art. 6 (1) (b) GDPR. The cookie is required to keep you signed in.',
          ],
        },
        {
          heading: 'Google sign-in',
          paragraphs: [
            'Google sign-in is optional. If you use it, Google learns that you used this service and gives us an ID token. We verify the token and store the subject identifier, email, and name to the extent Google sends them.',
            'Google’s policy: https://policies.google.com/privacy',
            'Google may process data in the United States. The legal basis is Art. 6 (1) (b) GDPR when you create or use the account this way.',
          ],
        },
        {
          heading: 'Email and verification codes',
          paragraphs: [
            'To confirm an email address and for sensitive changes (email, password, account deletion) we send a code through:',
            '[E-Mail-Anbieter]',
            'Only a hash of the code is stored, with its purpose and expiry. It becomes invalid after use or expiry. The legal basis is Art. 6 (1) (b) and (f) GDPR (providing the account and preventing abuse).',
          ],
        },
        {
          heading: 'Surveys and answers',
          paragraphs: [
            'Whoever creates a survey stores question text, options, and settings, including whether results are public and whether only one response is allowed. That person’s username can be visible to participants.',
            'People without an account can submit answers. What those answers contain is defined by the survey. Do not enter data you would not want others to see, especially when results are public.',
            'The access code you type is kept in sessionStorage so the same survey can be opened again in that tab. It is dropped when the tab closes.',
            'When only one response is allowed, the server sets an httpOnly cookie named osr_ plus the survey id. It lasts about two hours. The server stores only a hash of that value so the same browser cannot submit twice. The browser also marks that survey as submitted in localStorage.',
            'The legal basis is Art. 6 (1) (b) GDPR for the creator and Art. 6 (1) (f) GDPR for running the survey and limiting repeat responses.',
          ],
        },
        {
          heading: 'Display settings',
          paragraphs: [
            'Light or dark theme, accent color, and language (German or English) are stored in localStorage so the choice is still there on the next visit. These are not advertising cookies. The legal basis is Art. 6 (1) (f) GDPR and § 25 (2) TDDDG, because the storage applies the display you chose.',
          ],
        },
        {
          heading: 'Administration',
          paragraphs: [
            'People with the ADMIN role can view, edit, and delete user accounts in order to look after the demo. The legal basis is Art. 6 (1) (f) GDPR.',
          ],
        },
        {
          heading: 'Recipients',
          paragraphs: [
            'Data is shared only with parties that need it for the purposes above: the [Hosting-Anbieter], if the demo is hosted; the [E-Mail-Anbieter] for verification mail; Google, if you use Google sign-in; administrators of this instance.',
            'Data is not passed on for advertising.',
          ],
        },
        {
          heading: 'Retention',
          paragraphs: [
            'Account data stays until you delete the account or an administrator deletes it. That creator’s surveys and answers are removed with the account. Verification codes last only until they are used or they expire. The response cookie lasts about two hours. localStorage entries stay until you clear them in the browser or log out (logout removes the session hints, not theme and language).',
          ],
        },
        {
          heading: 'Your rights',
          paragraphs: [
            'You have the right to access, rectification, erasure, restriction, portability, and to object to processing based on Art. 6 (1) (f) GDPR. Where processing is based on consent, you may withdraw that consent for the future.',
            'You may lodge a complaint with a supervisory authority. For this demo that is: [Zuständige Aufsichtsbehörde].',
            'Deleting your own account is available in settings.',
          ],
        },
        {
          heading: 'Whether you must provide data',
          paragraphs: [
            'An account cannot be created without a username, email, and password — or a Google sign-in instead. Filling out a survey does not require an account. Which answers the survey asks for is decided by the person who created it.',
          ],
        },
        {
          heading: 'Automated decisions',
          paragraphs: [
            'There is no automated decision-making and no profiling within the meaning of Art. 22 GDPR.',
          ],
        },
      ],
    },
  },
}
