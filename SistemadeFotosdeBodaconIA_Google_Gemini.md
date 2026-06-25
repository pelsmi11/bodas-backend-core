7/5/26, 17:40 

Sistema de Fotos de Boda con IA - Google Gemini 

## Documento de Diseño Arquitectónico: Sistema de Captura y Proyección de Eventos en Tiempo Real 

## 1. Visión General del Sistema 

El sistema es una plataforma orientada a eventos sociales (bodas) que permite a los administradores crear eventos, generar códigos QR y moderar contenido. Los invitados pueden subir fotografías sin necesidad de registrarse para una experiencia sin fricciones. Las imágenes aprobadas por una IA de moderación se proyectan en tiempo real. 

## 2. Flujo de Autenticación Diferida ("Lazy Login" / Invitados Anónimos) 

1. Rastreo: El frontend genera un UUID (Guest ID) guardado en el localStorage . 

2. Subida Segura: El QR contiene un Token de Evento. El backend valida este token y genera una URL Prefirmada (Presigned URL) de Amazon S3. 

3. Fusión ("Merge"): Si el usuario inicia sesión con Cognito, el frontend avisa a Spring Boot, el cual hace un UPDATE en PostgreSQL uniendo el Guest ID al nuevo User ID de Cognito. 

## 3. Desglose de Microservicios y Frameworks 

- Microservicio A (Core & WebSockets): Spring Boot. Alojado en Amazon ECS (Fargate). 

- Microservicio B (Moderación IA): Node.js (AWS Lambda). Worker asíncrono. 

- Frontend: React.js. Alojado en Amazon S3 + CloudFront. 

- Base de Datos: PostgreSQL (Neon.com). 

## 4. Diseño Técnico del Worker de Moderación (Lambda + SQS + Node.js) 

1. Notificación (S3): El celular sube la foto y S3 dispara un evento s3:ObjectCreated:Put . 

2. Encolamiento (Amazon SQS): S3 envía el evento a una cola SQS para amortiguar picos de tráfico. 

3. Procesamiento: Lambda lee de SQS en lotes y llama a Amazon Rekognition. 

4. Tolerancia a fallos: Tras 3 intentos fallidos, los mensajes van a una Dead Letter Queue (DLQ). 

## 5. Seguridad de la Comunicación Interna (Webhook Lambda -> Spring Boot) 

Para notificar a Spring Boot que una foto fue aprobada, usamos un Webhook seguro: 

Estrategia A (Aplicación): Lambda y ECS comparten un secreto guardado en AWS Secrets Manager. Lambda lo envía en el Header HTTP. 

https://gemini.google.com/app/503705e24e89b0d8 

1/4 

7/5/26, 17:40 

Sistema de Fotos de Boda con IA - Google Gemini 

- Estrategia B (Red - Más Segura): Lambda se despliega en la misma VPC Privada que ECS y se comunica a través de un Internal Load Balancer protegido por Security Groups. 

## 6. Estructura de Repositorios y CI/CD 

1. bodas-frontend : React/Vite -> Github Actions -> S3 + CloudFront. 

2. bodas-backend-core : Spring Boot -> Github Actions -> ECR -> ECS (Fargate). 

3. bodas-workers-serverless : Node.js -> Github Actions -> AWS SAM (Lambda/SQS). 

4. bodas-infrastructure-iac : Terraform -> Aprovisiona toda la nube. 

## 7. Diseño de Base de Datos (PostgreSQL / Neon) 

Usuarios , Eventos , Participaciones (con guest_id y user_id ), Fotos . 

## 8. Diseño de Redes (Amazon VPC) 

- Public Subnet: NAT Gateway (con Elastic IP) y el Application Load Balancer Público. 

- Private Subnet: Contenedores ECS Fargate (Spring Boot). 

- Reglas Neon: Neon.com solo acepta tráfico de la Elastic IP del NAT Gateway. 

## 9. Infraestructura Completa a Desplegar (El Mapa de AWS) 

Esta es la lista exacta de recursos que debes crear (idealmente usando Terraform o la Consola de AWS) para que la arquitectura funcione. 

## 9.1. Capa de Redes (Networking) 

- 1 Amazon VPC: La red virtual principal. 

- 2 Subredes Públicas (Public Subnets): En distintas Zonas de Disponibilidad (AZs) para el Load Balancer. 

- 2 Subredes Privadas (Private Subnets): En distintas AZs para los contenedores de Spring Boot. 

- 1 Internet Gateway (IGW): Para que las subredes públicas tengan salida a internet. 

- 1 NAT Gateway + 1 Elastic IP: Ubicado en la subred pública, permite que Spring Boot (en la privada) salga a Internet (para conectarse a Neon PostgreSQL) de forma segura. 

## 9.2. Capa de Cómputo (Backend) 

- Amazon ECR (Elastic Container Registry): Un repositorio para guardar la imagen Docker de tu Spring Boot. 

- Amazon ECS Cluster: El clúster lógico. 

- ECS Task Definition & Service (Fargate): La definición que dice "Ejecuta mi imagen Docker usando X CPU y Y Memoria sin administrar servidores". 

- Application Load Balancer (ALB): Expuesto a internet en las subredes públicas. Recibe las peticiones HTTP/WebSocket y las reparte a los contenedores Fargate. 

https://gemini.google.com/app/503705e24e89b0d8 

2/4 

7/5/26, 17:40 

Sistema de Fotos de Boda con IA - Google Gemini 

## 9.3. Capa Serverless & Storage 

- Amazon S3 (Bucket Frontend): Aloja los archivos estáticos de React ( index.html , .js , .css ). 

- Amazon S3 (Bucket Fotos): Recibe las fotos de los invitados. 

- Amazon CloudFront (CDN): Caché global que apunta al Bucket Frontend para que la web cargue rápido. 

- Amazon SQS (Main Queue & DLQ): Recibe eventos del Bucket Fotos. 

- AWS Lambda: La función Node.js conectada a SQS y con permisos hacia Amazon Rekognition. 

## 9.4. Capa de Seguridad e Identidad 

- Amazon Cognito (User Pool): Para el registro y login con Google. 

- AWS Certificate Manager (ACM): Provee certificados SSL/TLS gratuitos. 

Security Groups (SG): 

- ALB_SG : Permite tráfico entrante por puerto 443 (HTTPS). 

- Fargate_SG : Permite tráfico entrante en puerto 8080 SOLO desde el ALB_SG . 

## 10. Integración de tu Dominio Personalizado (Namecheap -> AWS) 

Aprovechando que tienes el dominio hectormartinezmoreira.com en Namecheap, vamos a estructurar las URLs del proyecto de manera profesional. 

- Frontend (App Invitados/Pantalla): https://app.hectormartinezmoreira.com 

- Backend (API Spring Boot): https://api.hectormartinezmoreira.com 

## Paso 1: Migrar la gestión DNS a Amazon Route 53 

Para aprovechar la integración nativa de AWS (Alias Records) recomendada para el examen: 

1. En AWS, ve a Amazon Route 53 y crea una "Hosted Zone" (Zona Alojada) pública para hectormartinezmoreira.com . 

2. AWS te dará 4 servidores de nombres (Name Servers o NS), por ejemplo: ns-123.awsdns.com . 

3. Ve a tu panel de Namecheap, busca la configuración DNS de tu dominio y selecciona "Custom DNS". Pega ahí los 4 servidores de AWS. (A partir de este momento, AWS controla tu dominio). 

## Paso 2: Generar Certificados SSL Gratuitos (AWS Certificate Manager - ACM) 

Para que tus URLs tengan el candado verde (HTTPS - fundamental para WebSockets y permisos de cámara en móviles): 

## 1. Certificado para el Frontend (CloudFront): 

- Trampa de examen SAA-C03: Los certificados para CloudFront SIEMPRE deben solicitarse en la región us-east-1 (N. Virginia). 

- Solicita un certificado en us-east-1 para *.hectormartinezmoreira.com o específicamente para app.hectormartinezmoreira.com . 

- Valídalo mediante DNS (Route 53 creará el registro de validación automáticamente). 

https://gemini.google.com/app/503705e24e89b0d8 

3/4 

7/5/26, 17:40 

Sistema de Fotos de Boda con IA - Google Gemini 

2. Certificado para el Backend (Application Load Balancer): 

   - Solicita otro certificado en la región donde desplegaste tu VPC y ALB (ej. us-east-2 u otra). 

Solicítalo para api.hectormartinezmoreira.com y valídalo. 

## Paso 3: Conectar el Frontend a tu Dominio 

1. En la configuración de tu distribución de Amazon CloudFront, añade 

   - app.hectormartinezmoreira.com como Alternate Domain Name (CNAME). 

2. Selecciona el certificado ACM de us-east-1 que creaste en el Paso 2. 

3. Ve a Route 53, crea un registro tipo "A". 

4. En Route 53, activa el botón de "Alias" y selecciona tu distribución de CloudFront. 

## Paso 4: Conectar el Backend a tu Dominio 

1. En tu Application Load Balancer (ALB), crea un Listener en el puerto 443 (HTTPS) y asígnale el certificado ACM de tu región local. 

2. Ve a Route 53, crea un registro tipo "A" para api.hectormartinezmoreira.com . 

3. Activa el botón de "Alias" y selecciona tu Application Load Balancer. 

Con esto, cuando tu frontend en React haga una petición de login o WebSocket, la enviará a 

https://api.hectormartinezmoreira.com/... , viajando de forma segura (encriptada en 

https://gemini.google.com/app/503705e24e89b0d8 

4/4 

