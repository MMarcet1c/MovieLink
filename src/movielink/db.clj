(ns movielink.db
  (:require [next.jdbc :as jdbc]
            [buddy.hashers :as hashers])
  (:import [java.io File]))

(def db-file "movies.db")

(def db-spec {:dbtype "sqlite" :dbname db-file})

(defn db-exists? []
  (.exists (File. db-file)))

(defn setup-db []
  (when (not (db-exists?))
    (println "Creating database and seeding data...")
    ;; Create tables
    (jdbc/execute! db-spec ["CREATE TABLE IF NOT EXISTS users (
                               id INTEGER PRIMARY KEY AUTOINCREMENT,
                               username TEXT UNIQUE,
                               password_hash TEXT)"])
    (jdbc/execute! db-spec ["CREATE TABLE IF NOT EXISTS movies (
                               id INTEGER PRIMARY KEY AUTOINCREMENT,
                               title TEXT UNIQUE,
                               genres TEXT,
                               rating REAL,
                               description TEXT)"])
    (jdbc/execute! db-spec ["CREATE TABLE IF NOT EXISTS favorites (
                               id INTEGER PRIMARY KEY AUTOINCREMENT,
                               user_id INTEGER,
                               movie_id INTEGER,
                               UNIQUE(user_id, movie_id),
                               FOREIGN KEY(user_id) REFERENCES users(id),
                               FOREIGN KEY(movie_id) REFERENCES movies(id))"])
    (jdbc/execute! db-spec ["CREATE TABLE IF NOT EXISTS friends (
                             id INTEGER PRIMARY KEY AUTOINCREMENT,
                             user_id INTEGER,
                             friend_id INTEGER,
                             UNIQUE(user_id, friend_id),
                             FOREIGN KEY(user_id) REFERENCES users(id),
                             FOREIGN KEY(friend_id) REFERENCES users(id))"])

    ;; Seed data
    (doseq [movie [["The Matrix" "Action,Sci-Fi" 8.7 "A hacker discovers reality is simulated."]
                   ["Inception" "Action,Sci-Fi" 8.8 "A thief steals secrets through dreams."]
                   ["Titanic" "Romance,Drama" 7.8 "Love story on the ill-fated ship."]
                   ["The Godfather" "Crime,Drama" 9.2 "Mafia family saga."]
                   ["Interstellar" "Sci-Fi,Adventure,Drama" 8.6 "Space journey to save humanity."]
                   ["The Dark Knight" "Action,Crime,Drama" 9.0 "Batman faces the Joker in Gotham."]
                   ["Pulp Fiction" "Crime,Comedy,Drama" 8.9 "Multiple stories intertwine in crime world."]
                   ["Forrest Gump" "Drama,Romance,Comedy" 8.8 "Life story of Forrest Gump."]
                   ["Gladiator" "Action,Adventure,Drama" 8.5 "Roman general seeks revenge."]
                   ["The Shawshank Redemption" "Drama" 9.3 "Friendship in prison."]
                   ["Avatar" "Action,Sci-Fi,Adventure" 7.8 "Humans explore Pandora."]
                   ["Jurassic Park" "Action,Adventure,Sci-Fi" 8.1 "Dinosaurs recreated in a theme park."]
                   ["The Lion King" "Adventure,Drama" 8.5 "A lion cub becomes king."]
                   ["The Avengers" "Action,Sci-Fi,Adventure" 8.0 "Superheroes assemble to save the world."]
                   ["Toy Story" "Adventure,Comedy," 8.3 "Toys come to life."]
                   ["Finding Nemo" "Adventure,Comedy," 8.1 "A fish searches for his son."]
                   ["The Silence of the Lambs" "Crime,Horror" 8.6 "Hannibal Lecter helps catch a killer."]
                   ["Se7en" "Crime,Drama" 8.6 "Detectives hunt a serial killer."]
                   ["The Prestige" "Drama,Sci-Fi" 8.5 "Two magicians rival each other."]
                   ["The Departed" "Crime,Drama" 8.5 "Undercover cop and mole in crime syndicate."]
                   ["Braveheart" "Action,Drama" 8.3 "Scottish warrior fights for freedom."]
                   ["A Beautiful Mind" "Drama" 8.2 "Mathematician battles mental illness."]
                   ["The Social Network" "Drama" 7.7 "Creation of Facebook."]
                   ["La La Land" "Romance,Drama" 8.0 "Love story of musicians in LA."]
                   ["Mad Max: Fury Road" "Action,Adventure,Sci-Fi" 8.1 "Post-apocalyptic survival chase."]
                   ["Black Panther" "Action,Adventure,Sci-Fi" 7.3 "King of Wakanda faces threats."]
                   ["Guardians of the Galaxy" "Action,Adventure,Comedy,Sci-Fi" 8.0 "Space misfits become heroes."]
                   ["Coco" "Adventure,Comedy" 8.4 "Boy travels to the Land of the Dead."]
                   ["Spirited Away" "Adventure,Sci-Fi" 8.6 "Girl explores spirit world."]
                   ["Your Name" "Romance,Drama" 8.4 "Two teens mysteriously swap bodies."]
                   ["The Incredibles" "Action,Adventure" 8.0 "Superhero family saves the world."]
                   ["Shutter Island" "Drama" 8.2 "Detective investigates asylum."]
                   ["Django Unchained" "Drama,Action" 8.4 "Bounty hunter rescues his wife."]
                   ["The Wolf of Wall Street" "Comedy,Crime" 8.2 "Rise and fall of a stockbroker."]
                   ["Fight Club" "Drama" 8.8 "Man creates an underground fighting club."]
                   ["The Big Short" "Comedy,Drama" 7.8 "Financial crisis explained."]]]
      (jdbc/execute! db-spec
                     ["INSERT OR IGNORE INTO movies (title, genres, rating, description) VALUES (?, ?, ?, ?)"
                      (nth movie 0) (nth movie 1) (nth movie 2) (nth movie 3)]))))
