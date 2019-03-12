const { Profile, Profiles } = require("../domain/profiles");
const { get, all, exec } = require("../driver/database");

exports.init = async function init() {
  await exec(`
    create table profile (
      id integer primary key,
      user_id integer unique,
      display_name text,
      description text
    );
  `)
}

exports.list = async function list() {
  return new Profiles((await all(`
    select * from profile;
  `)).map(Profile.from));
}

exports.findByUserId = async function findByUserId(userId) {
  return await get(`
    select * from profile where user_id = ${userId};
  `);
}

exports.findById = async function findById(id) {
  return Profile.from(await get(`
    select * from profile where id = ${id};
  `));
}

exports.create = async function create(profile) {
  const { userId, displayName, description } = profile;
  await exec(`
    insert into profile (user_id, display_name, description)
      values (${userId}, "${displayName}", "${description}");
  `);
  return Profile.from(await get(`
    select * from profile user_id = ${userId};
  `));
}