<?php
// GENERATED CODE -- DO NOT EDIT!

namespace Zemberek\Normalization;

/**
 */
class NormalizationServiceClient extends \Grpc\BaseStub {

    /**
     * @param string $hostname hostname
     * @param array $opts channel options
     * @param \Grpc\Channel $channel (optional) re-use channel object
     */
    public function __construct($hostname, $opts, $channel = null) {
        parent::__construct($hostname, $opts, $channel);
    }

    /**
     * @param \Zemberek\Normalization\NormalizationRequest $argument input argument
     * @param array $metadata metadata
     * @param array $options call options
     */
    public function Normalize(\Zemberek\Normalization\NormalizationRequest $argument,
      $metadata = [], $options = []) {
        return $this->_simpleRequest('/zemberek.normalization.NormalizationService/Normalize',
        $argument,
        ['\Zemberek\Normalization\NormalizationResponse', 'decode'],
        $metadata, $options);
    }

}
