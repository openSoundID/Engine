package org.opensoundid.jpa;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.query.Query;

import org.opensoundid.jpa.entity.Also;
import org.opensoundid.jpa.entity.Bird;
import org.opensoundid.jpa.entity.Record;

public class JpaUtil {
    private static StandardServiceRegistry registry;
    private static SessionFactory sessionFactory;

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                // Create registry
                registry = new StandardServiceRegistryBuilder().configure().build();

                // Create MetadataSources
                MetadataSources sources = new MetadataSources(registry);

                // Create Metadata
                Metadata metadata = sources.getMetadataBuilder().build();

                // Create SessionFactory
                sessionFactory = metadata.getSessionFactoryBuilder().build();

            } catch (Exception e) {
                e.printStackTrace();
                if (registry != null) {
                    StandardServiceRegistryBuilder.destroy(registry);
                }
            }
         }
         return sessionFactory;
     }

     public static void shutdown() {
         if (registry != null) {
             StandardServiceRegistryBuilder.destroy(registry);
         }
     }
     
     public static void main(String[] args) {


         try (Session session = JpaUtil.getSessionFactory().openSession()) {
        	 Query<Bird> queryBird = session.createNamedQuery("Bird.findBySpeciesAndGenre", Bird.class).setParameter("genre", "Troglodytes").setParameter("species", "troglodyte");
        	 Bird bird = queryBird.getSingleResult();
        	 
        	 Query<Record> queryRecord = session.createNamedQuery("Record.findByBirdId", Record.class).setParameter("birdId", 7000);
        	 List<Record> records = queryRecord.getResultList();
        	 
        	 Query<Also> queryAlso = session.createNamedQuery("Also.findByBirdId", Also.class).setParameter("birdId", 7000);
        	 List<Also> birds = queryAlso.getResultList();
        	 
        	 System.out.println(bird.getFrName());
        	 
         } catch (Exception e) {
              e.printStackTrace();
         }
     }
     
     
 }                