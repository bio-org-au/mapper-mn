-- host
INSERT INTO mapper.host (id, host_name, preferred) VALUES (1, 'localhost:8080', true);
INSERT INTO mapper.host (id, host_name, preferred) VALUES (2, 'randomhost.com', false);
-- match uris
INSERT INTO mapper.match (id, uri, deprecated, updated_at, updated_by) VALUES (3, 'name/apni/54433', false, '2019-09-09 14:15:05.517000', 'pmcneil');
INSERT INTO mapper.match (id, uri, deprecated, updated_at, updated_by) VALUES (4, 'cgi-bin/apni?taxon_id=230687', true, '2019-09-09 14:15:47.545000', 'pmcneil');
INSERT INTO mapper.match (id, uri, deprecated, updated_at, updated_by) VALUES (5, 'name/apni/148297', false, '2019-09-09 14:16:22.807000', 'pmcneil');
INSERT INTO mapper.match (id, uri, deprecated, updated_at, updated_by) VALUES (6, 'Tieghemopanax macgillivrayi R.Vig.', false, null, null);
INSERT INTO mapper.match (id, uri, deprecated, updated_at, updated_by) VALUES (7, 'no-identifier/match', false, null, null);
INSERT INTO mapper.match (id, uri, deprecated, updated_at, updated_by) VALUES (8, 'tree/23/2222', false, '2019-09-09 04:16:22.807000', 'pmcneil');
-- match to host
INSERT INTO mapper.match_host (match_hosts_id, host_id) VALUES (3, 1);
INSERT INTO mapper.match_host (match_hosts_id, host_id) VALUES (4, 1);
INSERT INTO mapper.match_host (match_hosts_id, host_id) VALUES (5, 1);
INSERT INTO mapper.match_host (match_hosts_id, host_id) VALUES (6, 1);
INSERT INTO mapper.match_host (match_hosts_id, host_id) VALUES (7, 1);
INSERT INTO mapper.match_host (match_hosts_id, host_id) VALUES (8, 1);
-- identifiers
INSERT INTO mapper.identifier (id, id_number, name_space, object_type, deleted, reason_deleted, updated_at, updated_by, preferred_uri_id, version_number) VALUES (9, 148297, 'apni', 'name', true, 'Name has not been applied to Australian flora', '2019-09-09 14:13:58.741000', 'pmcneil', 6, null);
INSERT INTO mapper.identifier (id, id_number, name_space, object_type, deleted, reason_deleted, updated_at, updated_by, preferred_uri_id, version_number) VALUES (10, 54433, 'apni', 'name', false, null, '2019-09-09 14:12:21.950000', 'pmcneil', 4, null);
INSERT INTO mapper.identifier (id, id_number, name_space, object_type, deleted, reason_deleted, updated_at, updated_by, preferred_uri_id, version_number) VALUES (11, 77821, 'apni', 'name', false, null, null, null, 7, null);
INSERT INTO mapper.identifier (id, id_number, name_space, object_type, deleted, reason_deleted, updated_at, updated_by, preferred_uri_id, version_number) VALUES (12, 666, 'blah', 'name', false, null, null, null, null, null);
INSERT INTO mapper.identifier (id, id_number, name_space, object_type, deleted, reason_deleted, updated_at, updated_by, preferred_uri_id, version_number) VALUES (13, 2222, 'apni', 'treeElement', false, null, '2019-09-09 04:12:21.950000', 'pmcneil', 8, 23);
-- identifiers to match
INSERT INTO mapper.identifier_identities (match_id, identifier_id) VALUES (3, 10);
INSERT INTO mapper.identifier_identities (match_id, identifier_id) VALUES (4, 10);
INSERT INTO mapper.identifier_identities (match_id, identifier_id) VALUES (5, 9);
INSERT INTO mapper.identifier_identities (match_id, identifier_id) VALUES (6, 11);
INSERT INTO mapper.identifier_identities (match_id, identifier_id) VALUES (8, 13);
