/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unknownpotato.ohtu.miniproj.ui;

import com.unknownpotato.ohtu.miniproj.domain.Reference;
import com.unknownpotato.ohtu.miniproj.domain.ReferenceFactory;
import com.unknownpotato.ohtu.miniproj.domain.References;
import java.util.Scanner;

/**
 *
 * @author axwikstr
 */
public class TextUI {

    private static Scanner reader = new Scanner(System.in);
    private References references;
    
    public TextUI(References references) {
        this.references = references;
    }

    public void run() {
        int choice = 0;
        while (true) {
            System.out.println("Type 1 to add a new book type reference, 2 to list all book type references, 3 to export to BibTex or 4 to quit");
            try {
                choice = Integer.parseInt(reader.nextLine());
            } catch (Exception ex) {
                continue;
            }
            if (choice > 4 || choice < 1) {
                continue;
            } else if (choice == 1) {
                System.out.println("Author: ");
                String author = reader.nextLine();
                System.out.println("Title: ");
                String title = reader.nextLine();
                System.out.println("Publisher: ");
                String publisher = reader.nextLine();
                System.out.println("Year: ");
                String year = reader.nextLine();
                Reference ref = ReferenceFactory.createBookReference(author, title, publisher, year);
                System.out.println("You have added a new book type reference with the following information:");
                System.out.println("Author: " + ref.getField("author") + " Title: " + ref.getField("title")
                        + " Publisher: " + ref.getField("publisher") + " Year: " + ref.getField("year"));
                continue;
            } else if (choice == 2) {
                for (Reference reference : references.getReferences()) {
                    System.out.println("Author: " + reference.getField("author") + " Title: " + reference.getField("title")
                        + " Publisher: " + reference.getField("publisher") + " Year: " + reference.getField("year"));
                }
                continue;
            } else if (choice == 3) {
                System.out.println("To be implemented!");
                continue;
            } else if (choice == 4) {
                break;
            }
        }
    }
}
