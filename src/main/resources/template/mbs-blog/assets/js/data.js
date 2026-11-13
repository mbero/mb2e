/*
 * Sample content for local design work; mb2e writes its own data.js when it publishes.
 * TREE: left-panel folders { name, children } and notes { note: '<id>' }; NOTES: each note by id.
 * Sample block types: p, h2, ul (items), code, link (note); mb2e writes one html block per note.
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
    /* The "About me" page – opened from the bottom of the left panel, not shown in the tree */
    'about': {
      title: 'About me',
      category: 'About',
      date: '',
      tags: [],
      blocks: [
        { type: 'p', text: '[SHORT DESCRIPTION — who you are and what this blog is about]' }
      ]
    },

    'Databases/Indexes': {
      title: 'Indexes',
      category: 'Databases',
      date: '',
      tags: ['postgres', 'performance'],
      blocks: [
        { type: 'p', text: 'An index is a separate data structure that lets the database find rows without reading the whole table — it works like the index at the end of a book.' },
        { type: 'h2', text: 'When to add an index' },
        { type: 'ul', items: [
          'Columns often used in WHERE, JOIN and ORDER BY',
          'Foreign keys — PostgreSQL does not create indexes for them automatically',
          'Queries that return a small percentage of the table rows'
        ] },
        { type: 'h2', text: 'Index types in PostgreSQL' },
        { type: 'ul', items: [
          'B-tree — the default; equality and ranges (=, <, >, BETWEEN)',
          'Hash — equality comparisons only',
          'GIN — arrays, jsonb, full-text search',
          'GiST / SP-GiST — geometric data, ranges',
          'BRIN — very large tables with a natural order, e.g. time columns'
        ] },
        { type: 'h2', text: 'Example' },
        { type: 'code', text: 'CREATE INDEX CONCURRENTLY idx_orders_customer_id\n    ON orders (customer_id);' },
        { type: 'p', text: 'CONCURRENTLY builds the index without blocking writes to the table — it takes longer, but it is safer in production.' },
        { type: 'h2', text: 'Cost' },
        { type: 'p', text: 'Every index slows down INSERT, UPDATE and DELETE and takes disk space. Find and remove unused indexes regularly.' },
        { type: 'link', note: 'Databases/pg_stat' }
      ]
    },

    'Databases/Monitoring': {
      title: 'Monitoring DB performance',
      category: 'Databases',
      date: '',
      tags: ['postgres', 'monitoring'],
      blocks: [
        { type: 'p', text: 'Measure before you optimise. A slow database is usually a few specific queries, not "the whole database".' },
        { type: 'h2', text: 'What to measure' },
        { type: 'ul', items: [
          'The slowest and the most frequent queries — pg_stat_statements',
          'Active sessions and locks — pg_stat_activity',
          'Cache hit ratio — the share of reads served from memory instead of disk',
          'Table bloat and overdue VACUUM — pg_stat_user_tables'
        ] },
        { type: 'h2', text: 'Logging slow queries' },
        { type: 'code', text: '# postgresql.conf\nlog_min_duration_statement = 500   # ms' },
        { type: 'p', text: 'Every query that takes longer than 500 ms goes to the log together with its text.' },
        { type: 'h2', text: 'Execution plan' },
        { type: 'code', text: 'EXPLAIN (ANALYZE, BUFFERS)\nSELECT * FROM orders WHERE customer_id = 42;' },
        { type: 'p', text: 'ANALYZE actually runs the query and shows the real timings; BUFFERS shows how many pages came from memory and how many from disk. A Seq Scan on a large table with a small result is a sign of a missing index.' },
        { type: 'link', note: 'Databases/Indexes' }
      ]
    },

    'Databases/pg_stat': {
      title: 'pg_stat',
      category: 'Databases',
      date: '',
      tags: ['postgres', 'monitoring'],
      blocks: [
        { type: 'p', text: 'pg_stat_* are the built-in statistics views of PostgreSQL. They work like the dashboard of a car: they fix nothing, but they show where to look for the problem.' },
        { type: 'h2', text: 'The most important views' },
        { type: 'ul', items: [
          'pg_stat_activity — who is connected and what they run now',
          'pg_stat_user_tables — seq scans vs index scans, dead tuples, last VACUUM',
          'pg_stat_user_indexes — how often each index is used',
          'pg_stat_statements — aggregated query statistics (an extension; you must enable it)'
        ] },
        { type: 'h2', text: 'Top 10 slowest queries' },
        { type: 'code', text: 'SELECT query, calls, mean_exec_time\nFROM pg_stat_statements\nORDER BY mean_exec_time DESC\nLIMIT 10;' },
        { type: 'h2', text: 'Unused indexes' },
        { type: 'code', text: 'SELECT indexrelname, idx_scan\nFROM pg_stat_user_indexes\nWHERE idx_scan = 0;' },
        { type: 'p', text: 'The counters run from the last statistics reset — before you drop an "unused" index, make sure they cover a representative period.' },
        { type: 'link', note: 'Databases/Monitoring' }
      ]
    }
  }
};
