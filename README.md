# EcsFloralies
Cette application a pour but de faciliter la capture et l'archivage sur Google Drive des relevés
des compteurs d'eau chaude de ma residence.

Le premier écran permet de: 
- s'authentifier avec un compte Google. 
- chosir de prendre une photo du compteur avec la camera du téléphone ou avec un endoscope connecté
  au téléphone en USB. Ce qui s'avere utile pour les compteur difficiles d'accès. 

Dès que la photo est prise, un écran de sauvegarde s'affiche.
L'utilisateur doit alors sélectionner dans des listes les caractéristiques qui identifient le compteur 
parmis l'ensemble des compteurs de la copropriété
Ces caractéristique sont: Le nom de batiment, l'étage, l'appartement et la localisation des compteurs
dans l'appartement (WC, SDB, Cuisine)

Exemple: la photo A_ET2_F2_FACE_ASC_SDB.jpg est le compteur de la salle du  F2 "face ascenceur" au
deuxième étage du batiment A 

La photo est copiée sur le Google Drive associé à l'authentification dans un repertoire ECS_2022
situé à la racine

L'endoscope est le modèle Depstech. L'application ECSFloralies lance l'applicaton Depstech-View
