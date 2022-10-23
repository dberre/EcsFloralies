# EcsFloralies
Cette application a pour but de facliter la capture et l'archivage sur Google Drive des relevés
des compteurs d'eau chaude de ma residence.

Le premier écran permet de se s'authentifier avec un compte Google. 
Il permet aussi de chosir de prendre une photo du compteur avec la camera du
téléphone ou avec un endoscope connecté au téléphone en USB. Utile pour les compteur difficiles d'accès. 

Dès que la photo est prise, un écran de sauvegarde s'affiche.
L'utilisateur doit sélectionner dans des listes le nom de batiment,
l'étage, l'appartement et la localisation des compteurs dans l'appartement (WC, SDB, Cuisine)
De ces choix découle le nom qui sera donné à la photo.
Example A_ET2_F2_FACE_ASC_SDB.jpg est le batiment A, deuxième étage, F2 face ascenceur et compteur de la salle de bain
La photo est alors copiée sur le Google Drive associé à l'authentification dans un repertoire ECS_2022 situé à la racine

L'endoscope est le modèle Depstech. L'application ECSFloralies lance l'applicaton Depstech-View
