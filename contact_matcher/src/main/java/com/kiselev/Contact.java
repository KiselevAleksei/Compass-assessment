package com.kiselev;

import lombok.Data;

@Data
public class Contact {
    private int contactID;
    private String firstName;
    private String lastName;
    private String email;
    private int postalZip;
    private String address;

    public Contact(int contactID, String firstName, String lastName, String email, int postalZip, String address) {
        this.contactID = contactID;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.postalZip = postalZip;
        this.address = address;
    }

}
