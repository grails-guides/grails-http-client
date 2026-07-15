databaseChangeLog = {

    changeSet(author: 'guide', id: 'create-record-label') {
        createTable(tableName: 'record_label') {
            column(name: 'id', type: 'BIGINT', autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: 'version', type: 'BIGINT') {
                constraints(nullable: false)
            }
            column(name: 'name', type: 'VARCHAR(100)') {
                constraints(nullable: false, unique: true)
            }
        }
    }
}
