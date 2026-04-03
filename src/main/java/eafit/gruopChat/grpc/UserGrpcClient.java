package eafit.gruopChat.grpc;

import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class UserGrpcClient {

    @GrpcClient("user-service")
    private UserGrpcServiceGrpc.UserGrpcServiceBlockingStub userStub;

    public Optional<UserResponse> getUserById(String userId) {
        try {
            return Optional.of(userStub.getUserById(
                UserIdRequest.newBuilder().setUserId(userId).build()));
        } catch (StatusRuntimeException e) {
            System.err.println("gRPC ERROR getUserById: " + e.getStatus() + " - " + e.getMessage());

            return Optional.empty();
        }
    }


    public boolean existsUser(String userId) {
        try {
            return userStub.existsUser(
                UserIdRequest.newBuilder().setUserId(userId).build()).getExists();
        } catch (StatusRuntimeException e) {
            return false;
        }
    }
}