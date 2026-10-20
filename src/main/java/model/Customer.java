package model;

import java.time.LocalDateTime;

public final class Customer {
    private final long id; private String fullName, address, mobile; private final String passwordHash; private final LocalDateTime createdAt;
    public Customer(long id, String fullName, String address, String mobile, String passwordHash, LocalDateTime createdAt) {
        if(fullName==null||fullName.isBlank()||address==null||address.isBlank()||mobile==null||!mobile.matches("[0-9+() -]{7,20}")) throw new IllegalArgumentException("Enter a name, address and valid mobile number.");
        this.id=id; this.fullName=fullName.trim(); this.address=address.trim(); this.mobile=mobile.trim(); this.passwordHash=passwordHash; this.createdAt=createdAt;
    }
    public long getId(){return id;} public String getFullName(){return fullName;} public String getAddress(){return address;} public String getMobile(){return mobile;} public String getPasswordHash(){return passwordHash;} public LocalDateTime getCreatedAt(){return createdAt;}
    public void updateDetails(String name,String addr,String phone){ if(name==null||name.isBlank()||addr==null||addr.isBlank()||phone==null||!phone.matches("[0-9+() -]{7,20}")) throw new IllegalArgumentException("Invalid customer details."); fullName=name.trim();address=addr.trim();mobile=phone.trim(); }
}
