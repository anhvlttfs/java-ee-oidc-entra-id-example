package Models.Objects;

public class User {

    private final String preferred_username;
    private final String email;
    private final String name;
    private final String country;

    public User(String preferred_username, String email, String name, String country) {
        this.preferred_username = preferred_username;
        this.email = email;
        this.name = name;
        this.country = country;
    }

    public String getPreferred_username() {
        return preferred_username;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }
}
