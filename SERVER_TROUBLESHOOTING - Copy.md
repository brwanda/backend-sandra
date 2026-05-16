# Server Troubleshooting Guide

## 🚨 Common Issue: ERR_CONNECTION_REFUSED

### Error Message
```
GET http://localhost:8080/api/sub-committees net::ERR_CONNECTION_REFUSED
```

### Root Cause
The Spring Boot backend server is not running on port 8080.

## 🔧 Quick Fix

### Option 1: Use Startup Script
```bash
# Windows
./start-server.bat

# Linux/Mac
chmod +x start-server.sh
./start-server.sh
```

### Option 2: Manual Startup
```bash
cd eara_connect_new_backend-main-2
mvn spring-boot:run
```

### Option 3: Check Server Status
```bash
# Windows
./check-server.bat

# Manual check
curl http://localhost:8080/api/sub-committees
```

## 🔍 Verification Steps

### 1. Check Server is Running
```bash
# Should show port 8080 in use
netstat -an | grep 8080    # Linux/Mac
netstat -an | findstr 8080 # Windows
```

### 2. Test API Endpoints
```bash
# Health check
curl http://localhost:8080/actuator/health

# SubCommittee API
curl http://localhost:8080/api/sub-committees

# Should return JSON array like:
# [{"id":1,"name":"HR Subcommittee"}, ...]
```

### 3. Check Application Logs
Look for these startup messages:
```
✅ Started EaraconnectApplication in X.XXX seconds
✅ Tomcat started on port(s): 8080 (http)
```

## 🐛 Common Issues & Solutions

### Issue 1: Port Already in Use
```
Port 8080 was already in use
```

**Solution:**
```bash
# Find process using port 8080
netstat -ano | findstr :8080  # Windows
lsof -ti:8080                 # Linux/Mac

# Kill the process
taskkill /PID <PID> /F        # Windows
kill -9 <PID>                 # Linux/Mac
```

### Issue 2: Java Version Issues
```
Unsupported Java version
```

**Solution:**
- Ensure Java 17+ is installed
- Check: `java -version`
- Update JAVA_HOME if needed

### Issue 3: Maven Dependencies
```
Could not resolve dependencies
```

**Solution:**
```bash
mvn clean install
mvn dependency:resolve
```

### Issue 4: Database Connection Issues
```
Could not connect to database
```

**Solution:**
1. Check PostgreSQL is running
2. Verify connection settings in `application.properties`
3. Test database connection:
   ```sql
   psql -h localhost -p 5432 -U postgres -d eara_connect_db-final
   ```

## 📋 Startup Checklist

Before starting the server:

- [ ] Java 17+ installed
- [ ] Maven installed  
- [ ] PostgreSQL running
- [ ] Database `eara_connect_db-final` exists
- [ ] Port 8080 available
- [ ] All dependencies resolved (`mvn dependency:resolve`)

## 🎯 Expected Server Response

When the server is running correctly:

### SubCommittee API Response
```json
[
  {
    "id": 1,
    "name": "HR Subcommittee",
    "description": "Human Resources Management"
  },
  {
    "id": 2,
    "name": "IT Subcommittee", 
    "description": "Information Technology"
  }
]
```

### Health Check Response
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    }
  }
}
```

## 🚀 Development Workflow

### 1. Start Backend
```bash
cd eara_connect_new_backend-main-2
mvn spring-boot:run
```

### 2. Start Frontend  
```bash
cd eara_connect_new_frontend-main
npm start
```

### 3. Access Application
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- Admin Login: admin@earaconnect.com / admin123

## 📞 Need Help?

If the server still won't start:

1. Check the console output for specific error messages
2. Verify database connection settings
3. Ensure all required services are running
4. Check firewall settings for port 8080
5. Try restarting with clean build: `mvn clean spring-boot:run`

---

**Quick Commands:**
- Start: `mvn spring-boot:run`
- Check: `curl http://localhost:8080/api/sub-committees`
- Stop: `Ctrl+C` in terminal
