SELECT * FROM
  (SELECT ent2.id,ent2.name, 2 as 'type', 'all' as 'permissions' , ent.id as 'parent_id', ent.name as 'parent_name'
    FROM algator.entities as ent
    JOIN algator.owners as own ON ent.id = own.id_entity
    JOIN algator.users as usr ON own.id_owner=usr.id
    JOIN algator.entities as ent2 ON ent2.id_parent=ent.id
    WHERE usr.name='%1$s' AND ent.type=1 AND ent2.type=2) as e
  UNION
  (SELECT ent.id,ent.name, 2 as 'type', 'all' as 'permissions', pent.id as 'parent_id', pent.name as 'parent_name'
    FROM algator.entities as ent
    JOIN algator.owners as own ON ent.id = own.id_entity
    JOIN algator.users as usr ON own.id_owner=usr.id
    JOIN algator.entities as pent ON ent.id_parent=pent.id
    WHERE ent.type=2 AND usr.name='%1$s')
  UNION
  (SELECT ent2.id,ent2.name, 2 as 'type', perm.permission_code as 'permissions', ent.id as 'parent_id', ent.name as 'parent_name'
    FROM algator.entities as ent
    JOIN algator.permissions_users as pu ON ent.id = pu.id_entity
    JOIN algator.users as usr ON pu.id_user=usr.id
    JOIN algator.entities as ent2 ON ent2.id_parent=ent.id
    JOIN algator.permissions as perm ON perm.id=pu.id_permission
    WHERE usr.name='%1$s' AND ent.type=1 AND ent2.type=2)
  UNION
  (SELECT DISTINCT ent.id,ent.name, 2 as 'type', perm.permission_code as 'permissions', pent.id as 'parent_id', pent.name as 'parent_name'
    FROM algator.entities as ent
    JOIN algator.permissions_users as pu ON ent.id=pu.id_entity
    JOIN algator.users as usr ON usr.id=pu.id_user
    JOIN algator.permissions as perm ON perm.id=pu.id_permission
    JOIN algator.entities as pent ON ent.id_parent=pent.id
    WHERE ent.type=2 AND usr.name='%1$s')
  UNION
  (SELECT distinct ent2.id,ent2.name, 2 as 'type', perm.permission_code as 'permissions', ent.id as 'parent_id', ent.name as 'parent_name'
    FROM algator.users as usr
    JOIN algator.group_users as gu ON usr.id=gu.id_user
    JOIN algator.groups as grp ON gu.id_group=grp.id
    JOIN algator.permissions_group as pg ON pg.id_group=grp.id
    JOIN algator.permissions as perm ON perm.id=pg.id_permission
    JOIN algator.entities as ent ON ent.id=pg.id_entity
    JOIN algator.entities as ent2 ON ent.id=ent2.id_parent
    WHERE ent.type=1 AND ent2.type=2 AND usr.name='%1$s')
  UNION
  (SELECT distinct ent.id,ent.name, 2 as 'type', perm.permission_code as 'permissions', pent.id as 'parent_id', pent.name as 'parent_name'
    FROM algator.users as usr
    JOIN algator.group_users as gu ON usr.id=gu.id_user
    JOIN algator.groups as grp ON gu.id_group=grp.id
    JOIN algator.permissions_group as pg ON pg.id_group=grp.id
    JOIN algator.permissions as perm ON perm.id=pg.id_permission
    JOIN algator.entities as ent ON ent.id=pg.id_entity
    JOIN algator.entities as pent ON ent.id_parent=pent.id
    WHERE ent.type=2 AND usr.name='%1$s')