SELECT * FROM (SELECT DISTINCT ent.id,ent.name, 1 as 'type', 'all' as 'permissions', 0 as 'parent_id','' as 'parent_name'
  FROM algator.u_entities as ent
  JOIN algator.u_owners as own ON ent.id = own.id_entity
  JOIN algator.u_users as usr ON own.id_owner=usr.id
  WHERE usr.name='%1$s' AND ent.type=1) as a
  UNION
  (SELECT DISTINCT ent.id,ent.name, 1 as 'type', perm.permission_code as 'permissions', 0 as 'parent_id','' as 'parent_name'
  FROM algator.u_entities as ent
  JOIN algator.u_permissions_users as pu ON ent.id=pu.id_entity
  JOIN algator.u_users as usr ON usr.id=pu.id_user
  JOIN algator.u_permissions as perm ON perm.id=pu.id_permission
  WHERE ent.type=1 AND usr.name='%1$s')
  UNION
  (SELECT DISTINCT ent.id,ent.name, 1 as 'type', perm.permission_code as 'permissions', 0 as 'parent_id','' as 'parent_name'
  FROM algator.u_users as usr
  JOIN algator.u_group_users as gu ON usr.id=gu.id_user
  JOIN algator.u_groups as grp ON gu.id_group=grp.id
  JOIN algator.u_permissions_group as pg ON pg.id_group=grp.id
  JOIN algator.u_permissions as perm ON perm.id=pg.id_permission
  JOIN algator.u_entities as ent ON ent.id=pg.id_entity
  WHERE ent.type=1 AND usr.name='%1$s')