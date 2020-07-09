package grpcCaller

import (
	"DragonDE/config"
	"log"

	"google.golang.org/grpc"
)

func Init() **grpc.ClientConn {
	var conn *grpc.ClientConn
	conn, err := grpc.Dial(config.Get().ZemberekGrpcServer, grpc.WithInsecure())
	if err != nil {
		log.Fatalf("did not connect: %s", err)
	}

	return &conn
}

func Close(conn **grpc.ClientConn) {
	(**conn).Close()
}
