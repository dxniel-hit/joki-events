package com.uq.jokievents.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "ticketorders")
public class TicketOrder {

    @Id
    private String id;
    private String idTicket;
    private Date purchaseDate;

    // Constructor
    public TicketOrder() {

    }

    public TicketOrder(String id, String idTicket, Date purchaseDate) {
        this.id = id;
        this.idTicket = idTicket;
        this.purchaseDate = purchaseDate;
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdTicket() {
        return idTicket;
    }

    public void setIdTicket(String idTicket) {
        this.idTicket = idTicket;
    }

    public Date getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(Date purchaseDate) {
        this.purchaseDate = purchaseDate;
    }
}