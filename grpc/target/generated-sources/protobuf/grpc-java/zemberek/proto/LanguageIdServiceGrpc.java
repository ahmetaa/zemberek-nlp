package zemberek.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.18.0)",
    comments = "Source: language_id.proto")
public final class LanguageIdServiceGrpc {

  private LanguageIdServiceGrpc() {}

  public static final String SERVICE_NAME = "zemberek.langid.LanguageIdService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<zemberek.proto.LanguageIdRequest,
      zemberek.proto.LanguageIdResponse> getDetectMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Detect",
      requestType = zemberek.proto.LanguageIdRequest.class,
      responseType = zemberek.proto.LanguageIdResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<zemberek.proto.LanguageIdRequest,
      zemberek.proto.LanguageIdResponse> getDetectMethod() {
    io.grpc.MethodDescriptor<zemberek.proto.LanguageIdRequest, zemberek.proto.LanguageIdResponse> getDetectMethod;
    if ((getDetectMethod = LanguageIdServiceGrpc.getDetectMethod) == null) {
      synchronized (LanguageIdServiceGrpc.class) {
        if ((getDetectMethod = LanguageIdServiceGrpc.getDetectMethod) == null) {
          LanguageIdServiceGrpc.getDetectMethod = getDetectMethod = 
              io.grpc.MethodDescriptor.<zemberek.proto.LanguageIdRequest, zemberek.proto.LanguageIdResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "zemberek.langid.LanguageIdService", "Detect"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  zemberek.proto.LanguageIdRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  zemberek.proto.LanguageIdResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new LanguageIdServiceMethodDescriptorSupplier("Detect"))
                  .build();
          }
        }
     }
     return getDetectMethod;
  }

  private static volatile io.grpc.MethodDescriptor<zemberek.proto.LanguageIdRequest,
      zemberek.proto.LanguageIdResponse> getDetectFastMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DetectFast",
      requestType = zemberek.proto.LanguageIdRequest.class,
      responseType = zemberek.proto.LanguageIdResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<zemberek.proto.LanguageIdRequest,
      zemberek.proto.LanguageIdResponse> getDetectFastMethod() {
    io.grpc.MethodDescriptor<zemberek.proto.LanguageIdRequest, zemberek.proto.LanguageIdResponse> getDetectFastMethod;
    if ((getDetectFastMethod = LanguageIdServiceGrpc.getDetectFastMethod) == null) {
      synchronized (LanguageIdServiceGrpc.class) {
        if ((getDetectFastMethod = LanguageIdServiceGrpc.getDetectFastMethod) == null) {
          LanguageIdServiceGrpc.getDetectFastMethod = getDetectFastMethod = 
              io.grpc.MethodDescriptor.<zemberek.proto.LanguageIdRequest, zemberek.proto.LanguageIdResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "zemberek.langid.LanguageIdService", "DetectFast"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  zemberek.proto.LanguageIdRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  zemberek.proto.LanguageIdResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new LanguageIdServiceMethodDescriptorSupplier("DetectFast"))
                  .build();
          }
        }
     }
     return getDetectFastMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static LanguageIdServiceStub newStub(io.grpc.Channel channel) {
    return new LanguageIdServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static LanguageIdServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new LanguageIdServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static LanguageIdServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new LanguageIdServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class LanguageIdServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void detect(zemberek.proto.LanguageIdRequest request,
        io.grpc.stub.StreamObserver<zemberek.proto.LanguageIdResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDetectMethod(), responseObserver);
    }

    /**
     */
    public void detectFast(zemberek.proto.LanguageIdRequest request,
        io.grpc.stub.StreamObserver<zemberek.proto.LanguageIdResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDetectFastMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getDetectMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                zemberek.proto.LanguageIdRequest,
                zemberek.proto.LanguageIdResponse>(
                  this, METHODID_DETECT)))
          .addMethod(
            getDetectFastMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                zemberek.proto.LanguageIdRequest,
                zemberek.proto.LanguageIdResponse>(
                  this, METHODID_DETECT_FAST)))
          .build();
    }
  }

  /**
   */
  public static final class LanguageIdServiceStub extends io.grpc.stub.AbstractStub<LanguageIdServiceStub> {
    private LanguageIdServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private LanguageIdServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LanguageIdServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new LanguageIdServiceStub(channel, callOptions);
    }

    /**
     */
    public void detect(zemberek.proto.LanguageIdRequest request,
        io.grpc.stub.StreamObserver<zemberek.proto.LanguageIdResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDetectMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void detectFast(zemberek.proto.LanguageIdRequest request,
        io.grpc.stub.StreamObserver<zemberek.proto.LanguageIdResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDetectFastMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class LanguageIdServiceBlockingStub extends io.grpc.stub.AbstractStub<LanguageIdServiceBlockingStub> {
    private LanguageIdServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private LanguageIdServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LanguageIdServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new LanguageIdServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public zemberek.proto.LanguageIdResponse detect(zemberek.proto.LanguageIdRequest request) {
      return blockingUnaryCall(
          getChannel(), getDetectMethod(), getCallOptions(), request);
    }

    /**
     */
    public zemberek.proto.LanguageIdResponse detectFast(zemberek.proto.LanguageIdRequest request) {
      return blockingUnaryCall(
          getChannel(), getDetectFastMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class LanguageIdServiceFutureStub extends io.grpc.stub.AbstractStub<LanguageIdServiceFutureStub> {
    private LanguageIdServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private LanguageIdServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LanguageIdServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new LanguageIdServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<zemberek.proto.LanguageIdResponse> detect(
        zemberek.proto.LanguageIdRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDetectMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<zemberek.proto.LanguageIdResponse> detectFast(
        zemberek.proto.LanguageIdRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDetectFastMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_DETECT = 0;
  private static final int METHODID_DETECT_FAST = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final LanguageIdServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(LanguageIdServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_DETECT:
          serviceImpl.detect((zemberek.proto.LanguageIdRequest) request,
              (io.grpc.stub.StreamObserver<zemberek.proto.LanguageIdResponse>) responseObserver);
          break;
        case METHODID_DETECT_FAST:
          serviceImpl.detectFast((zemberek.proto.LanguageIdRequest) request,
              (io.grpc.stub.StreamObserver<zemberek.proto.LanguageIdResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class LanguageIdServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    LanguageIdServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return zemberek.proto.LanguageId.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("LanguageIdService");
    }
  }

  private static final class LanguageIdServiceFileDescriptorSupplier
      extends LanguageIdServiceBaseDescriptorSupplier {
    LanguageIdServiceFileDescriptorSupplier() {}
  }

  private static final class LanguageIdServiceMethodDescriptorSupplier
      extends LanguageIdServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    LanguageIdServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (LanguageIdServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new LanguageIdServiceFileDescriptorSupplier())
              .addMethod(getDetectMethod())
              .addMethod(getDetectFastMethod())
              .build();
        }
      }
    }
    return result;
  }
}
