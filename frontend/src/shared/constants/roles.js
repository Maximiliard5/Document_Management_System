// Mirrors com.example.dms.entity.Role. Values match the wire format exactly
// (Jackson serializes the enum as its name), so `Role.ADMIN === 'ADMIN'`.
export const Role = Object.freeze({
  ADMIN: 'ADMIN',
  USER: 'USER',
})
