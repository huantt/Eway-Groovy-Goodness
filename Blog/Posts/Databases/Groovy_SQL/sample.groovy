import groovy.sql.*

def username = 'groovy', password = 'groovy', database = 'groovy', server = 'localhost'

// Create connection to MySQL with classic JDBC DriverManager.
def db = Sql.newInstance("jdbc:mysql://$server/$database", username, password, 'com.mysql.jdbc.Driver')

// Or we can create a connection with a DataSource (also via JNDI possible)
def ds = new com.mysql.jdbc.jdbc2.optional.MysqlDataSource(
    databaseName: database, user: username, password: password, serverName: server
)
assert 'jdbc:mysql://localhost:3306/groovy' == ds.url
def dbDS = new Sql(ds)

// Create a new table
db.execute 'drop table if exists languages'
// We can use multi-line strings to create readable SQL in our code.
db.execute '''
    create table languages(
        id integer not null auto_increment,
        name varchar(20) not null,
        primary key(id)
    )
'''

// Fill table with data in different ways.
// First a normal statement.
db.execute 'insert into languages values(null, "Groovy")'
assert 1 == db.updateCount
// String with extra parameters will become a prepared statement.
db.execute 'insert into languages values(null, ?)', ['Java']
assert 1 == db.updateCount
// GString will become a prepared statement.
def langValue = 'JRuby'
db.execute "insert into languages values(null, $langValue)"
assert 1 == db.updateCount

// With executeInsert we get the generated id(s) back.
def insertedIds = db.executeInsert 'insert into languages values(null, "Scalaa")'
assert 4 == insertedIds[0][0]

// executeUpdate return number of rows affected.
def old = 'Scalaa', new = 'Scala'
def updated = db.executeUpdate "update languages set name=$new where name=$old"
assert 1 == updated


// Now let's get data from the table Groovy style.
// With rows we get a list of GroovyResultSet objects and this means we can
// use column names to access data in a row.
def all = db.rows('select * from languages')
assert 4 == all.size()
assert ['Groovy', 'Java', 'JRuby', 'Scala'] == all.collect{ it.name }
assert ['Groovy', 'JRuby'] == all.findAll{ it.name ~= /y/ }

// With eachRow we can use a closure to do something with each row.
// The closure parameter is also of type GroovyResultSet.
def maxId = 3
db.eachRow("select id, name from languages where id < $maxId") { row ->
    if (row.id == 1) assert 'Groovy' == row.name
    if (row.id == 2) assert 'Java' == row.name
}
db.eachRow("select name from language where name=?", ['Java']) { 
    assert 'Java' == it.name
}

def countRows = db.firstRow("select count(*) as numberOfRows from languages")
assert 4 == countRows.numberOfRows
