/*
 * Treść bloga.
 *
 * TREE  – struktura kategorii (lewy panel). Folder: { name, children: [...] }.
 *         Notatka: { note: '<id notatki z NOTES>' }.
 * NOTES – notatki. Klucz = id (używany w adresie: #/Databases/Indexes).
 *
 * Typy bloków treści:
 *   { type: 'p',    text: '...' }              akapit
 *   { type: 'h2',   text: '...' }              nagłówek sekcji
 *   { type: 'ul',   items: ['...', '...'] }    lista
 *   { type: 'code', text: '...' }              blok kodu
 *   { type: 'link', note: '<id>' }             „Powiązane: …” (link do innej notatki)
 *
 * Pierwszy akapit notatki jest wyświetlany jako lead pod tytułem.
 * Pole date jest opcjonalne (format dowolny, np. '2026-11-06').
 */
window.BLOG_DATA = {
  defaultNote: 'Databases/Indexes',

  TREE: [
    { name: 'AI', children: [] },
    { name: 'AWS', children: [
      { name: 'AWS - Practice', children: [] },
      { name: 'AWS Knowledge Base', children: [] }
    ] },
    { name: 'CI_CD', children: [] },
    { name: 'Databases', children: [
      { note: 'Databases/Indexes' },
      { note: 'Databases/Monitoring' },
      { note: 'Databases/pg_stat' }
    ] },
    { name: 'Dev_Ops', children: [] },
    { name: 'docs', children: [] },
    { name: 'DRAFTS', children: [] },
    { name: 'Engineering_Journal', children: [] },
    { name: 'Flowable', children: [] },
    { name: 'Images', children: [] },
    { name: 'Interview_Prep', children: [] },
    { name: 'Java', children: [] },
    { name: 'Microservices', children: [] },
    { name: 'Other', children: [] },
    { name: 'Search', children: [] },
    { name: 'Spring', children: [] },
    { name: 'System design', children: [] }
  ],

  NOTES: {
    /* Strona „About me” – otwierana z dołu lewego panelu, nie występuje w drzewie */
    'about': {
      title: 'About me',
      category: 'About',
      date: '',
      tags: [],
      blocks: [
        { type: 'p', text: '[KRÓTKI OPIS — kim jesteś i o czym jest ten blog]' }
      ]
    },

    'Databases/Indexes': {
      title: 'Indexes',
      category: 'Databases',
      date: '',
      tags: ['postgres', 'performance'],
      blocks: [
        { type: 'p', text: 'Indeks to osobna struktura danych, która pozwala bazie znaleźć wiersze bez czytania całej tabeli — działa jak skorowidz na końcu książki.' },
        { type: 'h2', text: 'Kiedy dodać indeks' },
        { type: 'ul', items: [
          'Kolumny często używane w WHERE, JOIN i ORDER BY',
          'Klucze obce — PostgreSQL nie tworzy dla nich indeksów automatycznie',
          'Zapytania zwracające niewielki procent wierszy tabeli'
        ] },
        { type: 'h2', text: 'Typy indeksów w PostgreSQL' },
        { type: 'ul', items: [
          'B-tree — domyślny; równość i zakresy (=, <, >, BETWEEN)',
          'Hash — wyłącznie porównania równości',
          'GIN — tablice, jsonb, wyszukiwanie pełnotekstowe',
          'GiST / SP-GiST — dane geometryczne, zakresy',
          'BRIN — bardzo duże tabele z naturalnym porządkiem, np. kolumny czasu'
        ] },
        { type: 'h2', text: 'Przykład' },
        { type: 'code', text: 'CREATE INDEX CONCURRENTLY idx_orders_customer_id\n    ON orders (customer_id);' },
        { type: 'p', text: 'CONCURRENTLY buduje indeks bez blokowania zapisów do tabeli — trwa dłużej, ale jest bezpieczniejsze na produkcji.' },
        { type: 'h2', text: 'Koszt' },
        { type: 'p', text: 'Każdy indeks spowalnia INSERT, UPDATE i DELETE oraz zajmuje miejsce na dysku. Nieużywane indeksy warto regularnie wykrywać i usuwać.' },
        { type: 'link', note: 'Databases/pg_stat' }
      ]
    },

    'Databases/Monitoring': {
      title: 'Monitoring DB performance',
      category: 'Databases',
      date: '',
      tags: ['postgres', 'monitoring'],
      blocks: [
        { type: 'p', text: 'Zanim zaczniesz optymalizować, zmierz. Wolna baza to zwykle kilka konkretnych zapytań, a nie „cała baza”.' },
        { type: 'h2', text: 'Co mierzyć' },
        { type: 'ul', items: [
          'Najwolniejsze i najczęściej wykonywane zapytania — pg_stat_statements',
          'Aktywne sesje i blokady — pg_stat_activity',
          'Cache hit ratio — odsetek odczytów z pamięci zamiast z dysku',
          'Rozrost tabel i zaległy VACUUM — pg_stat_user_tables'
        ] },
        { type: 'h2', text: 'Logowanie wolnych zapytań' },
        { type: 'code', text: '# postgresql.conf\nlog_min_duration_statement = 500   # ms' },
        { type: 'p', text: 'Każde zapytanie trwające dłużej niż 500 ms trafi do logów razem z treścią.' },
        { type: 'h2', text: 'Plan wykonania' },
        { type: 'code', text: 'EXPLAIN (ANALYZE, BUFFERS)\nSELECT * FROM orders WHERE customer_id = 42;' },
        { type: 'p', text: 'ANALYZE faktycznie wykonuje zapytanie i pokazuje rzeczywiste czasy; BUFFERS — ile stron przeczytano z pamięci, a ile z dysku. Seq Scan na dużej tabeli przy małym wyniku to sygnał, że brakuje indeksu.' },
        { type: 'link', note: 'Databases/Indexes' }
      ]
    },

    'Databases/pg_stat': {
      title: 'pg_stat',
      category: 'Databases',
      date: '',
      tags: ['postgres', 'monitoring'],
      blocks: [
        { type: 'p', text: 'pg_stat_* to wbudowane widoki statystyk PostgreSQL. Działają jak deska rozdzielcza w samochodzie: niczego nie naprawiają, ale pokazują, gdzie szukać problemu.' },
        { type: 'h2', text: 'Najważniejsze widoki' },
        { type: 'ul', items: [
          'pg_stat_activity — kto jest połączony i co teraz wykonuje',
          'pg_stat_user_tables — seq scany vs index scany, martwe krotki, ostatni VACUUM',
          'pg_stat_user_indexes — jak często używany jest każdy indeks',
          'pg_stat_statements — zagregowane statystyki zapytań (rozszerzenie, trzeba je włączyć)'
        ] },
        { type: 'h2', text: 'Top 10 najwolniejszych zapytań' },
        { type: 'code', text: 'SELECT query, calls, mean_exec_time\nFROM pg_stat_statements\nORDER BY mean_exec_time DESC\nLIMIT 10;' },
        { type: 'h2', text: 'Nieużywane indeksy' },
        { type: 'code', text: 'SELECT indexrelname, idx_scan\nFROM pg_stat_user_indexes\nWHERE idx_scan = 0;' },
        { type: 'p', text: 'Liczniki zbierane są od ostatniego resetu statystyk — zanim usuniesz „nieużywany” indeks, upewnij się, że obejmują reprezentatywny okres.' },
        { type: 'link', note: 'Databases/Monitoring' }
      ]
    }
  }
};
