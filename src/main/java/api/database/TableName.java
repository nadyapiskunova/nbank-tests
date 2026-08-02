package api.database;

public enum TableName {
        CUSTOMERS("customers"),
        ACCOUNTS("accounts"),
        TRANSACTIONS("transactions");

        private final String value;

        TableName(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
}
